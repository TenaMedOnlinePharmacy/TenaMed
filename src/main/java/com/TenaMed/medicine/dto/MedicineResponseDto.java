package com.TenaMed.medicine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineResponseDto {

    private UUID id;
    private String name;
    private String genericName;
    private String category;
    private String dosageForm;
    private String therapeuticClass;
    private String schedule;
    private boolean needManualReview;
    private BigDecimal doseValue;
    private String doseUnit;
    private String regulatoryCode;
    private boolean requiresPrescription;
    private String indications;
    private String contraindications;
    private String sideEffects;
    private String dosageInstructions;
    private String pregnancyCategory;
    private List<UUID> allergenIds;
    private List<UUID> dopingRuleIds;
}
