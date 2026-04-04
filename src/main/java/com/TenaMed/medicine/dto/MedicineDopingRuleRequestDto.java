package com.TenaMed.medicine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MedicineDopingRuleRequestDto {

    @NotBlank(message = "Ruleset is required")
    private String ruleset;

    @NotNull(message = "Ruleset year is required")
    private Integer rulesetYear;

    @NotBlank(message = "Status is required")
    private String status;

    private String notes;
}