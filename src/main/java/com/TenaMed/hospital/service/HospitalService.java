package com.TenaMed.hospital.service;

import com.TenaMed.doctor.dto.DoctorResponseDto;
import com.TenaMed.hospital.dto.HospitalRequestDto;
import com.TenaMed.hospital.dto.HospitalResponseDto;
import com.TenaMed.invitation.dto.InvitationResponseDto;

import java.util.List;
import java.util.UUID;

public interface HospitalService {

    HospitalResponseDto createHospital(HospitalRequestDto dto);

    HospitalResponseDto createHospitalForOwner(HospitalRequestDto dto, UUID ownerId);

    HospitalResponseDto getHospitalById(UUID hospitalId);

    HospitalResponseDto updateHospital(UUID hospitalId, HospitalRequestDto dto);

    HospitalResponseDto verifyHospital(UUID hospitalId);

    List<DoctorResponseDto> getHospitalDoctors(UUID hospitalId);

    InvitationResponseDto inviteDoctor(UUID hospitalId, String email);
}
