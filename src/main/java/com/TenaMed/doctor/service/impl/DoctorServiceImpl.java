package com.TenaMed.doctor.service.impl;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.common.exception.ResourceNotFoundException;
import com.TenaMed.common.exception.UnauthorizedException;
import com.TenaMed.common.security.CurrentUserProvider;
import com.TenaMed.doctor.dto.DoctorRequestDto;
import com.TenaMed.doctor.dto.DoctorResponseDto;
import com.TenaMed.doctor.entity.Doctor;
import com.TenaMed.doctor.entity.DoctorStatus;
import com.TenaMed.doctor.mapper.DoctorMapper;
import com.TenaMed.doctor.repository.DoctorRepository;
import com.TenaMed.doctor.service.DoctorService;
import com.TenaMed.hospital.entity.Hospital;
import com.TenaMed.hospital.repository.HospitalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorMapper doctorMapper;
    private final HospitalRepository hospitalRepository;
    private final CurrentUserProvider currentUserProvider;

    public DoctorServiceImpl(DoctorRepository doctorRepository,
                             DoctorMapper doctorMapper,
                             HospitalRepository hospitalRepository,
                             CurrentUserProvider currentUserProvider) {
        this.doctorRepository = doctorRepository;
        this.doctorMapper = doctorMapper;
        this.hospitalRepository = hospitalRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional
    public DoctorResponseDto createDoctorFromInvite(UUID userId, UUID hospitalId, DoctorRequestDto dto) {
        if (userId == null) {
            throw new BadRequestException("userId is required");
        }
        if (hospitalId == null) {
            throw new BadRequestException("hospitalId is required");
        }
        validateDoctorRequest(dto);

        if (doctorRepository.existsByUserId(userId)) {
            throw new BadRequestException("Doctor profile already exists for this user");
        }

        String normalizedLicense = normalize(dto.getLicenseNumber());
        if (doctorRepository.existsByLicenseNumberIgnoreCase(normalizedLicense)) {
            throw new BadRequestException("Doctor license number already exists");
        }

        hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found: " + hospitalId));

        Doctor doctor = doctorMapper.toEntity(dto);
        doctor.setUserId(userId);
        doctor.setHospitalId(hospitalId);
        doctor.setLicenseNumber(normalizedLicense);
        doctor.setSpecialization(normalize(dto.getSpecialization()));
        doctor.setStatus(DoctorStatus.PENDING);

        Doctor saved = doctorRepository.save(doctor);
        return doctorMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponseDto getDoctorById(UUID doctorId) {
        Doctor doctor = getDoctorEntityById(doctorId);
        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponseDto getMyProfile(UUID userId) {
        if (userId == null) {
            throw new BadRequestException("userId is required");
        }

        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for user: " + userId));
        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional
    public DoctorResponseDto verifyDoctor(UUID doctorId) {
        Doctor doctor = getDoctorEntityById(doctorId);

        Hospital hospital = hospitalRepository.findById(doctor.getHospitalId())
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found: " + doctor.getHospitalId()));

        UUID currentUserId = currentUserProvider.getCurrentUserId();
        boolean isOwner = hospital.getOwnerId().equals(currentUserId);
        if (!isOwner && !currentUserProvider.hasRole("ADMIN")) {
            throw new UnauthorizedException("Only hospital owner or admin can verify doctor");
        }

        doctor.setStatus(DoctorStatus.ACTIVE);
        doctor.setVerifiedBy(currentUserId);
        Doctor saved = doctorRepository.save(doctor);
        return doctorMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponseDto> getDoctorsByHospital(UUID hospitalId) {
        if (hospitalId == null) {
            throw new BadRequestException("hospitalId is required");
        }

        return doctorRepository.findByHospitalId(hospitalId).stream()
                .map(doctorMapper::toResponse)
                .toList();
    }

    private Doctor getDoctorEntityById(UUID doctorId) {
        if (doctorId == null) {
            throw new BadRequestException("doctorId is required");
        }

        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));
    }

    private void validateDoctorRequest(DoctorRequestDto dto) {
        if (dto == null) {
            throw new BadRequestException("Doctor payload is required");
        }
        if (isBlank(dto.getLicenseNumber())) {
            throw new BadRequestException("licenseNumber is required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
