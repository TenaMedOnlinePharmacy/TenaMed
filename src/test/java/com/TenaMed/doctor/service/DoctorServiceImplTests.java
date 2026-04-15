package com.TenaMed.doctor.service;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.common.exception.UnauthorizedException;
import com.TenaMed.common.security.CurrentUserProvider;
import com.TenaMed.doctor.dto.DoctorRequestDto;
import com.TenaMed.doctor.dto.DoctorResponseDto;
import com.TenaMed.doctor.entity.Doctor;
import com.TenaMed.doctor.entity.DoctorStatus;
import com.TenaMed.doctor.mapper.DoctorMapper;
import com.TenaMed.doctor.repository.DoctorRepository;
import com.TenaMed.doctor.service.impl.DoctorServiceImpl;
import com.TenaMed.hospital.entity.Hospital;
import com.TenaMed.hospital.repository.HospitalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorServiceImplTests {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DoctorMapper doctorMapper;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private DoctorServiceImpl doctorService;

    @Test
    void shouldCreateDoctorFromInviteWithPendingStatus() {
        UUID userId = UUID.randomUUID();
        UUID hospitalId = UUID.randomUUID();

        DoctorRequestDto dto = new DoctorRequestDto();
        dto.setLicenseNumber("  DOC-2001 ");
        dto.setSpecialization("  Cardiology  ");

        Doctor mapped = new Doctor();
        Doctor saved = new Doctor();
        saved.setId(UUID.randomUUID());
        saved.setUserId(userId);
        saved.setHospitalId(hospitalId);
        saved.setLicenseNumber("DOC-2001");
        saved.setSpecialization("Cardiology");
        saved.setStatus(DoctorStatus.PENDING);

        DoctorResponseDto response = new DoctorResponseDto();
        response.setId(saved.getId());
        response.setUserId(userId);
        response.setHospitalId(hospitalId);
        response.setLicenseNumber(saved.getLicenseNumber());
        response.setSpecialization(saved.getSpecialization());
        response.setStatus(saved.getStatus());

        Hospital hospital = new Hospital();
        hospital.setId(hospitalId);

        when(doctorRepository.existsByUserId(userId)).thenReturn(false);
        when(doctorRepository.existsByLicenseNumberIgnoreCase("DOC-2001")).thenReturn(false);
        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
        when(doctorMapper.toEntity(dto)).thenReturn(mapped);
        when(doctorRepository.save(any(Doctor.class))).thenReturn(saved);
        when(doctorMapper.toResponse(saved)).thenReturn(response);

        DoctorResponseDto actual = doctorService.createDoctorFromInvite(userId, hospitalId, dto);

        ArgumentCaptor<Doctor> captor = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorRepository).save(captor.capture());
        Doctor persisted = captor.getValue();

        assertEquals(DoctorStatus.PENDING, persisted.getStatus());
        assertEquals(userId, persisted.getUserId());
        assertEquals(hospitalId, persisted.getHospitalId());
        assertEquals("DOC-2001", persisted.getLicenseNumber());
        assertEquals("Cardiology", persisted.getSpecialization());

        assertEquals(saved.getId(), actual.getId());
        assertEquals(DoctorStatus.PENDING, actual.getStatus());
    }

    @Test
    void shouldRejectDoctorVerificationWhenUserIsNotOwnerOrAdmin() {
        UUID doctorId = UUID.randomUUID();
        UUID hospitalId = UUID.randomUUID();

        Doctor doctor = new Doctor();
        doctor.setId(doctorId);
        doctor.setHospitalId(hospitalId);

        Hospital hospital = new Hospital();
        hospital.setId(hospitalId);
        hospital.setOwnerId(UUID.randomUUID());

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
        when(currentUserProvider.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(currentUserProvider.hasRole("ADMIN")).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> doctorService.verifyDoctor(doctorId));

        assertTrue(ex.getMessage().contains("owner or admin"));
    }

    @Test
    void shouldVerifyDoctorWhenUserOwnsHospital() {
        UUID doctorId = UUID.randomUUID();
        UUID hospitalId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Doctor doctor = new Doctor();
        doctor.setId(doctorId);
        doctor.setHospitalId(hospitalId);
        doctor.setStatus(DoctorStatus.PENDING);

        Hospital hospital = new Hospital();
        hospital.setId(hospitalId);
        hospital.setOwnerId(ownerId);

        Doctor saved = new Doctor();
        saved.setId(doctorId);
        saved.setHospitalId(hospitalId);
        saved.setStatus(DoctorStatus.ACTIVE);
        saved.setVerifiedBy(ownerId);

        DoctorResponseDto response = new DoctorResponseDto();
        response.setId(doctorId);
        response.setHospitalId(hospitalId);
        response.setStatus(DoctorStatus.ACTIVE);

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
        when(currentUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(doctorRepository.save(any(Doctor.class))).thenReturn(saved);
        when(doctorMapper.toResponse(saved)).thenReturn(response);

        DoctorResponseDto actual = doctorService.verifyDoctor(doctorId);

        assertEquals(DoctorStatus.ACTIVE, actual.getStatus());
    }

    @Test
    void shouldRejectDoctorCreationWhenUserAlreadyHasProfile() {
        UUID userId = UUID.randomUUID();
        UUID hospitalId = UUID.randomUUID();
        DoctorRequestDto dto = new DoctorRequestDto();
        dto.setLicenseNumber("DOC-333");

        when(doctorRepository.existsByUserId(userId)).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> doctorService.createDoctorFromInvite(userId, hospitalId, dto));

        assertTrue(ex.getMessage().contains("already exists"));
    }
}
