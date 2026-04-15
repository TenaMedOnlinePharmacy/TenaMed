package com.TenaMed.patient.dto;

import java.util.UUID;

public record UpdateAllergyDto(
        UUID allergenId,
        String severity
) {
}
