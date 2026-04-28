package com.TenaMed.medicine.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MedicineNameCategoryResponseDto {

    private String name;
    private String category;
}
