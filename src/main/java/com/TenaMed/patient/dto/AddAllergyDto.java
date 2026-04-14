package com.TenaMed.patient.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AddAllergyDto {

    @NotNull
    private UUID allergenId;

    private String severity;
}
