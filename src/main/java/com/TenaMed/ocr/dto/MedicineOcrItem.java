package com.TenaMed.ocr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicineOcrItem {
    private String name;
    private Integer quantity;
    private String instruction;
}
