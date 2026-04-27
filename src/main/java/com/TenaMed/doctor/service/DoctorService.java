package com.TenaMed.doctor.service;

import com.TenaMed.doctor.dto.DoctorRequestDto;
import com.TenaMed.doctor.dto.DoctorResponseDto;

import java.util.List;
import java.util.UUID;

public interface DoctorService {

    DoctorResponseDto createDoctorFromInvite(UUID userId, UUID hospitalId, DoctorRequestDto dto);

    DoctorResponseDto getDoctorById(UUID doctorId);

    DoctorResponseDto getMyProfile(UUID userId);

    DoctorResponseDto verifyDoctor(UUID doctorId);

    DoctorResponseDto verifyDoctorForOwner(UUID ownerId, UUID doctorId);

    List<DoctorResponseDto> getDoctorsByHospital(UUID hospitalId);
}
