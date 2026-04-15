package com.TenaMed.doctor.mapper;

import com.TenaMed.doctor.dto.DoctorRequestDto;
import com.TenaMed.doctor.dto.DoctorResponseDto;
import com.TenaMed.doctor.entity.Doctor;
import org.springframework.stereotype.Component;

@Component
public class DoctorMapper {

    public Doctor toEntity(DoctorRequestDto dto) {
        Doctor doctor = new Doctor();
        doctor.setLicenseNumber(dto.getLicenseNumber());
        doctor.setSpecialization(dto.getSpecialization());
        return doctor;
    }

    public DoctorResponseDto toResponse(Doctor doctor) {
        DoctorResponseDto dto = new DoctorResponseDto();
        dto.setId(doctor.getId());
        dto.setUserId(doctor.getUserId());
        dto.setHospitalId(doctor.getHospitalId());
        dto.setLicenseNumber(doctor.getLicenseNumber());
        dto.setSpecialization(doctor.getSpecialization());
        dto.setStatus(doctor.getStatus());
        return dto;
    }
}
