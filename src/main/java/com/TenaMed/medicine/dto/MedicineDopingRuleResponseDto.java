package com.TenaMed.medicine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineDopingRuleResponseDto {

    private UUID id;
    private UUID medicineId;
    private String ruleset;
    private Integer rulesetYear;
    private String status;
    private String notes;
}