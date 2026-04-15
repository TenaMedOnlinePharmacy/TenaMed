package com.TenaMed.hospital.dto;

import com.TenaMed.hospital.entity.HospitalStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class HospitalResponseDto {
    private UUID id;
    private String name;
    private String licenseNumber;
    private String licenseImageUrl;
    private UUID ownerId;
    private HospitalStatus status;
    private LocalDateTime createdAt;
}
