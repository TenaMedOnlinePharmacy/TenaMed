package com.TenaMed.patient.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PatientDto(
        UUID id,
        String fullName,
        String phone,
        String uniqueCode,
        LocalDateTime createdAt
) {
}
