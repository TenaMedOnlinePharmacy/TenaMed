package com.TenaMed.patient.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class MedicineAllergyMatchResponse {
    private UUID allergenId;
    private String allergenName;
    private String allergenDescription;
    private String severity;
}
