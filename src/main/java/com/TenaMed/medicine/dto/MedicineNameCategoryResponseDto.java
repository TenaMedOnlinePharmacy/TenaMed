package com.TenaMed.medicine.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class MedicineNameCategoryResponseDto {

    private UUID medicineId;
    private String name;
    private String category;
}
