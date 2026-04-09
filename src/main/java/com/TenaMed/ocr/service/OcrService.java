package com.TenaMed.ocr.service;

import com.TenaMed.Normalization.model.NormalizedOcrResultDto;
import com.TenaMed.Normalization.service.OcrDrugNormalizationService;
import com.TenaMed.ocr.dto.OcrResultDto;
import com.TenaMed.ocr.integration.OcrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OcrService {

    private final OcrClient ocrClient;
    private final double confidenceThreshold;
    private final OcrDrugNormalizationService ocrDrugNormalizationService;

    public OcrService(
            OcrClient ocrClient,
            @Value("${ocr.confidence-threshold}") double confidenceThreshold, OcrDrugNormalizationService ocrDrugNormalizationService
    ) {
        this.ocrClient = ocrClient;
        this.confidenceThreshold = confidenceThreshold;
        this.ocrDrugNormalizationService = ocrDrugNormalizationService;
    }

    public OcrResultDto process(String imageUrl) {
        OcrResultDto result = ocrClient.processPrescription(imageUrl);

        if (result == null) {
            return new OcrResultDto(false, 0.0, java.util.List.of(), null);
        }

        if (result.getConfidence() < confidenceThreshold) {
            return new OcrResultDto(false, result.getConfidence(), result.getMedicines(), result.getPrescription());
        }

        return result;
    }
    public NormalizedOcrResultDto processOcrResult(OcrResultDto ocrResult) {
        return ocrDrugNormalizationService.normalize(ocrResult);
    }
}
