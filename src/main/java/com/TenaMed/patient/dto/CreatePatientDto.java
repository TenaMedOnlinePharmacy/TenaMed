package com.TenaMed.patient.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePatientDto(
        @NotBlank String fullName,
        @NotBlank String phone,
        String uniqueCode
) {
}
