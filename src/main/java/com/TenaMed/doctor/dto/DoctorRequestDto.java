package com.TenaMed.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DoctorRequestDto {

    @NotBlank
    private String licenseNumber;

    private String specialization;
}
