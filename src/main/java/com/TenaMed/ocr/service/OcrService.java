package com.TenaMed.ocr.service;

import com.TenaMed.ocr.dto.OcrResultDto;
import com.TenaMed.ocr.integration.OcrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OcrService {

    private final OcrClient ocrClient;
    private final double confidenceThreshold;

    public OcrService(
            OcrClient ocrClient,
            @Value("${ocr.confidence-threshold}") double confidenceThreshold
    ) {
        this.ocrClient = ocrClient;
        this.confidenceThreshold = confidenceThreshold;
    }

    public OcrResultDto process(String imageUrl) {
        OcrResultDto result = ocrClient.processPrescription(imageUrl);

        if (result == null) {
            return new OcrResultDto(false, 0.0, java.util.List.of());
        }

        if (result.getConfidence() < confidenceThreshold) {
            return new OcrResultDto(false, result.getConfidence(), result.getMedicines());
        }

        return result;
    }
}
