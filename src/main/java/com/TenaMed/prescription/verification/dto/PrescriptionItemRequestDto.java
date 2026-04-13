package com.TenaMed.prescription.verification.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PrescriptionItemRequestDto {

    @NotNull
    private UUID medicineId;

    @NotNull
    @Min(1)
    private Integer quantity;

    private String strength;
    private String form;
    private String instructions;
}
