package com.TenaMed.admin.dto;

import lombok.Data;

@Data
public class OcrStatsResponse {
    private long totalProcessed;
    private Double avgConfidence;
    private long passedCount;
    private long failedCount;
}
