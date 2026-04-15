package com.TenaMed.hospital.service.impl;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.common.exception.ResourceNotFoundException;
import com.TenaMed.common.exception.UnauthorizedException;
import com.TenaMed.common.security.CurrentUserProvider;
import com.TenaMed.doctor.dto.DoctorResponseDto;
import com.TenaMed.doctor.service.DoctorService;
import com.TenaMed.hospital.dto.HospitalRequestDto;
import com.TenaMed.hospital.dto.HospitalResponseDto;
import com.TenaMed.hospital.entity.Hospital;
import com.TenaMed.hospital.entity.HospitalStatus;
import com.TenaMed.hospital.mapper.HospitalMapper;
import com.TenaMed.hospital.repository.HospitalRepository;
import com.TenaMed.hospital.service.HospitalService;
import com.TenaMed.invitation.dto.InvitationResponseDto;
import com.TenaMed.invitation.service.InvitationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class HospitalServiceImpl implements HospitalService {

    private final HospitalRepository hospitalRepository;
    private final HospitalMapper hospitalMapper;
    private final DoctorService doctorService;
    private final InvitationService invitationService;
    private final CurrentUserProvider currentUserProvider;

    public HospitalServiceImpl(HospitalRepository hospitalRepository,
                               HospitalMapper hospitalMapper,
                               DoctorService doctorService,
                               InvitationService invitationService,
                               CurrentUserProvider currentUserProvider) {
        this.hospitalRepository = hospitalRepository;
        this.hospitalMapper = hospitalMapper;
        this.doctorService = doctorService;
        this.invitationService = invitationService;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional
    public HospitalResponseDto createHospital(HospitalRequestDto dto) {
        validateHospitalRequest(dto);

        String normalizedLicense = normalize(dto.getLicenseNumber());
        if (hospitalRepository.existsByLicenseNumberIgnoreCase(normalizedLicense)) {
            throw new BadRequestException("Hospital license number already exists");
        }

        Hospital hospital = hospitalMapper.toEntity(dto);
        hospital.setName(normalize(dto.getName()));
        hospital.setLicenseNumber(normalizedLicense);
        hospital.setOwnerId(currentUserProvider.getCurrentUserId());
        hospital.setStatus(HospitalStatus.PENDING);

        Hospital saved = hospitalRepository.save(hospital);
        return hospitalMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public HospitalResponseDto getHospitalById(UUID hospitalId) {
        Hospital hospital = getHospitalEntityById(hospitalId);
        return hospitalMapper.toResponse(hospital);
    }

    @Override
    @Transactional
    public HospitalResponseDto updateHospital(UUID hospitalId, HospitalRequestDto dto) {
        validateHospitalRequest(dto);

        Hospital hospital = getHospitalEntityById(hospitalId);
        assertOwnerOrAdmin(hospital);

        String normalizedLicense = normalize(dto.getLicenseNumber());
        if (!hospital.getLicenseNumber().equalsIgnoreCase(normalizedLicense)
                && hospitalRepository.existsByLicenseNumberIgnoreCase(normalizedLicense)) {
            throw new BadRequestException("Hospital license number already exists");
        }

        hospital.setName(normalize(dto.getName()));
        hospital.setLicenseNumber(normalizedLicense);

        Hospital saved = hospitalRepository.save(hospital);
        return hospitalMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public HospitalResponseDto verifyHospital(UUID hospitalId) {
        assertAdmin();

        Hospital hospital = getHospitalEntityById(hospitalId);
        hospital.setStatus(HospitalStatus.ACTIVE);
        hospital.setVerifiedBy(currentUserProvider.getCurrentUserId());

        Hospital saved = hospitalRepository.save(hospital);
        return hospitalMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponseDto> getHospitalDoctors(UUID hospitalId) {
        getHospitalEntityById(hospitalId);
        return doctorService.getDoctorsByHospital(hospitalId);
    }

    @Override
    @Transactional
    public InvitationResponseDto inviteDoctor(UUID hospitalId, String email) {
        Hospital hospital = getHospitalEntityById(hospitalId);
        assertOwnerOrAdmin(hospital);

        if (hospital.getStatus() != HospitalStatus.ACTIVE) {
            throw new BadRequestException("Hospital must be ACTIVE before inviting doctors");
        }

        return invitationService.createDoctorInvitation(hospitalId, email);
    }

    private Hospital getHospitalEntityById(UUID hospitalId) {
        if (hospitalId == null) {
            throw new BadRequestException("hospitalId is required");
        }
        return hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found: " + hospitalId));
    }

    private void validateHospitalRequest(HospitalRequestDto dto) {
        if (dto == null) {
            throw new BadRequestException("Hospital payload is required");
        }
        if (isBlank(dto.getName())) {
            throw new BadRequestException("name is required");
        }
        if (isBlank(dto.getLicenseNumber())) {
            throw new BadRequestException("licenseNumber is required");
        }
    }

    private void assertAdmin() {
        if (!currentUserProvider.hasRole("ADMIN")) {
            throw new UnauthorizedException("Admin role is required for this action");
        }
    }

    private void assertOwnerOrAdmin(Hospital hospital) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        boolean isOwner = currentUserId.equals(hospital.getOwnerId());
        if (!isOwner && !currentUserProvider.hasRole("ADMIN")) {
            throw new UnauthorizedException("You are not allowed to modify this hospital");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
