package com.TenaMed.Normalization.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedMedicine {
    private String originalName;
    private String normalizedName;
    private MatchType matchType;
    private double confidence;
    private boolean needsReview;
}
