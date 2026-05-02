package com.TenaMed.medicine.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MedicineRequestDto {

    @NotBlank(message = "Medicine name is required")
    private String name;

    private String genericName;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Dosage form is required")
    private String dosageForm;

    private String therapeuticClass;

    private String schedule;

    @NotNull(message = "Manual review flag is required")
    private Boolean needManualReview;

    private BigDecimal doseValue;

    private String doseUnit;

    private String regulatoryCode;

    private String indications;

    private String contraindications;

    private String sideEffects;

    private String dosageInstructions;

    @Size(max = 1, message = "Pregnancy category must be a single character")
    private String pregnancyCategory;

    private boolean requiresPrescription;

    private List<String> allergens;
}
