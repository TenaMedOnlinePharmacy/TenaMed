package com.TenaMed.verification.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrescriptionItemRequestDto {

    @NotBlank
    private String medicineName;

    @NotNull
    @Min(1)
    private Integer quantity;

    private String strength;
    private String form;
    private String instructions;
}
