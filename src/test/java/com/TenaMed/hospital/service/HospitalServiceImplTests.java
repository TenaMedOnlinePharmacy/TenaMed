package com.TenaMed.hospital.service;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.common.exception.UnauthorizedException;
import com.TenaMed.common.security.CurrentUserProvider;
import com.TenaMed.doctor.service.DoctorService;
import com.TenaMed.hospital.dto.HospitalRequestDto;
import com.TenaMed.hospital.dto.HospitalResponseDto;
import com.TenaMed.hospital.entity.Hospital;
import com.TenaMed.hospital.entity.HospitalStatus;
import com.TenaMed.hospital.mapper.HospitalMapper;
import com.TenaMed.hospital.repository.HospitalRepository;
import com.TenaMed.hospital.service.impl.HospitalServiceImpl;
import com.TenaMed.invitation.dto.InvitationResponseDto;
import com.TenaMed.invitation.service.InvitationService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HospitalServiceImplTests {

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private HospitalMapper hospitalMapper;

    @Mock
    private DoctorService doctorService;

    @Mock
    private InvitationService invitationService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private HospitalServiceImpl hospitalService;

    @Test
    void shouldCreateHospitalWithPendingStatusAndCurrentOwner() {
        UUID currentUserId = UUID.randomUUID();
        HospitalRequestDto dto = new HospitalRequestDto();
        dto.setName("  St. Gabriel Hospital  ");
        dto.setLicenseNumber("  LIC-1001  ");

        Hospital mapped = new Hospital();
        mapped.setId(UUID.randomUUID());

        Hospital saved = new Hospital();
        saved.setId(mapped.getId());
        saved.setName("St. Gabriel Hospital");
        saved.setLicenseNumber("LIC-1001");
        saved.setOwnerId(currentUserId);
        saved.setStatus(HospitalStatus.PENDING);

        HospitalResponseDto responseDto = new HospitalResponseDto();
        responseDto.setId(saved.getId());
        responseDto.setName(saved.getName());
        responseDto.setLicenseNumber(saved.getLicenseNumber());
        responseDto.setOwnerId(saved.getOwnerId());
        responseDto.setStatus(saved.getStatus());

        when(hospitalRepository.existsByLicenseNumberIgnoreCase("LIC-1001")).thenReturn(false);
        when(hospitalMapper.toEntity(dto)).thenReturn(mapped);
        when(currentUserProvider.getCurrentUserId()).thenReturn(currentUserId);
        when(hospitalRepository.save(any(Hospital.class))).thenReturn(saved);
        when(hospitalMapper.toResponse(saved)).thenReturn(responseDto);

        HospitalResponseDto actual = hospitalService.createHospital(dto);

        ArgumentCaptor<Hospital> captor = ArgumentCaptor.forClass(Hospital.class);
        verify(hospitalRepository).save(captor.capture());
        Hospital persisted = captor.getValue();

        assertEquals(HospitalStatus.PENDING, persisted.getStatus());
        assertEquals(currentUserId, persisted.getOwnerId());
        assertEquals("St. Gabriel Hospital", persisted.getName());
        assertEquals("LIC-1001", persisted.getLicenseNumber());

        assertEquals(saved.getId(), actual.getId());
        assertEquals(HospitalStatus.PENDING, actual.getStatus());
    }

    @Test
    void shouldRejectHospitalVerificationWhenCurrentUserIsNotAdmin() {
        UUID hospitalId = UUID.randomUUID();
        when(currentUserProvider.hasRole("ADMIN")).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> hospitalService.verifyHospital(hospitalId));

        assertTrue(ex.getMessage().contains("Admin role is required"));
        verify(hospitalRepository, never()).findById(any());
        verify(hospitalRepository, never()).save(any());
    }

    @Test
    void shouldRejectDoctorInvitationWhenHospitalIsNotActive() {
        UUID hospitalId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Hospital hospital = new Hospital();
        hospital.setId(hospitalId);
        hospital.setOwnerId(ownerId);
        hospital.setStatus(HospitalStatus.PENDING);

        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
        when(currentUserProvider.getCurrentUserId()).thenReturn(ownerId);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> hospitalService.inviteDoctorForOwner(hospitalId, "doc@example.com"));

        assertTrue(ex.getMessage().contains("Hospital must be ACTIVE"));
        verify(invitationService, never()).createDoctorInvitation(any(), any());
    }

    @Test
    void shouldInviteDoctorForActiveHospitalOwnedByCurrentUser() {
        UUID hospitalId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Hospital hospital = new Hospital();
        hospital.setId(hospitalId);
        hospital.setOwnerId(ownerId);
        hospital.setStatus(HospitalStatus.ACTIVE);

        InvitationResponseDto invitation = new InvitationResponseDto();
        invitation.setHospitalId(hospitalId);
        invitation.setEmail("doc@example.com");

        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
        when(currentUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(invitationService.createDoctorInvitation(hospitalId, "doc@example.com")).thenReturn(invitation);

        InvitationResponseDto actual = hospitalService.inviteDoctorForOwner(hospitalId, "doc@example.com");

        assertEquals("doc@example.com", actual.getEmail());
        assertEquals(hospitalId, actual.getHospitalId());
    }
}
