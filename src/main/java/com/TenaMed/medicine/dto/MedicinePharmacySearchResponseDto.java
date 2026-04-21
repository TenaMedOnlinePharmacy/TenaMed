package com.TenaMed.medicine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicinePharmacySearchResponseDto {

    private String medicineName;
    private String pharmacyLegalName;
    private BigDecimal price;
    private String medicineCategory;
    private String imageUrl;
    private String indications;
    private String contraindications;
    private String sideEffects;
}