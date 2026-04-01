package com.TenaMed.ocr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.TenaMed.ocr.dto.MedicineOcrItem;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrResultDto {
    private boolean success;
    private double confidence;
    private List<MedicineOcrItem> medicines;
}
