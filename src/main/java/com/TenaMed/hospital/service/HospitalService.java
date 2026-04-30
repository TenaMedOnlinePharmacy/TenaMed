package com.TenaMed.hospital.service;

import com.TenaMed.doctor.dto.DoctorResponseDto;
import com.TenaMed.doctor.entity.DoctorStatus;
import com.TenaMed.hospital.dto.HospitalDoctorResponseDto;
import com.TenaMed.hospital.dto.HospitalRequestDto;

import com.TenaMed.hospital.dto.HospitalResponseDto;
import com.TenaMed.hospital.dto.HospitalStatisticsDto;
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

    InvitationResponseDto inviteDoctorForOwner(UUID ownerId, String email);

    HospitalStatisticsDto getHospitalStatistics(UUID hospitalId, UUID ownerId);

    List<HospitalDoctorResponseDto> getHospitalDoctorsByStatus(UUID hospitalId, List<DoctorStatus> statuses);

    void acceptDoctor(UUID doctorId, UUID ownerId);

    void rejectDoctor(UUID doctorId, UUID ownerId);
}
