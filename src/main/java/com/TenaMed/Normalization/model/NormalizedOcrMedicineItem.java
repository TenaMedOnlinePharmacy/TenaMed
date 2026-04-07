package com.TenaMed.Normalization.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedOcrMedicineItem {
    private String originalName;
    private String normalizedName;
    private MatchType matchType;
    private double confidence;
    private double ocrConfidence;
    private boolean needsReview;
    private Integer quantity;
    private String instruction;
}
