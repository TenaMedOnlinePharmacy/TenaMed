package com.TenaMed.hospital.dto;

import com.TenaMed.doctor.entity.DoctorStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class HospitalDoctorResponseDto {
    private UUID doctorId;
    private String name;
    private String specialization;
    private String licenseNumber;
    private DoctorStatus status;
}
