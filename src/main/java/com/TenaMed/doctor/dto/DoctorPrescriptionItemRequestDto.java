package com.TenaMed.doctor.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DoctorPrescriptionItemRequestDto {

    @NotNull
    private UUID medicineId;

    @NotBlank
    private String form;

    @NotBlank
    private String instruction;

    @NotBlank
    private String strength;

    @NotNull
    @Min(1)
    private Integer quantity;
}
