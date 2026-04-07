package com.TenaMed.Normalization.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NormalizedMedicine {
    private String originalName;
    private String normalizedName;
    private MatchType matchType;
    private double confidence;
    private Double ocrConfidence;
    private boolean needsReview;

    public NormalizedMedicine(String originalName, String normalizedName, MatchType matchType, double confidence, boolean needsReview) {
        this(originalName, normalizedName, matchType, confidence, null, needsReview);
    }

    public NormalizedMedicine(
            String originalName,
            String normalizedName,
            MatchType matchType,
            double confidence,
            Double ocrConfidence,
            boolean needsReview
    ) {
        this.originalName = originalName;
        this.normalizedName = normalizedName;
        this.matchType = matchType;
        this.confidence = confidence;
        this.ocrConfidence = ocrConfidence;
        this.needsReview = needsReview;
    }
}
