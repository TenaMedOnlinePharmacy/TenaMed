package com.TenaMed.Normalization.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedOcrResultDto {
    private boolean success;
    private double confidence;
    private List<NormalizedOcrMedicineItem> medicines;
}
