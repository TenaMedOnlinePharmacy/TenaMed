package com.TenaMed.doctor.dto;

import com.TenaMed.doctor.entity.DoctorStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class DoctorResponseDto {
    private UUID id;
    private UUID userId;
    private UUID hospitalId;
    private String licenseNumber;
    private String specialization;
    private DoctorStatus status;
}
