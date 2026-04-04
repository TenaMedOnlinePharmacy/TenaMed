package com.TenaMed.medicine.dto;

import lombok.Data;

@Data
public class MedicineSearchDto {

    private String keyword;
    private String categoryName;
    private String dosageFormName;
    private String therapeuticClass;
    private Boolean requiresPrescription;
}
