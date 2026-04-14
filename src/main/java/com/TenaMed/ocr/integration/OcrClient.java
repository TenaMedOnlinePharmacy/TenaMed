package com.TenaMed.ocr.integration;

import com.TenaMed.ocr.dto.OcrResultDto;

import java.util.UUID;

public interface OcrClient {
    OcrResultDto processPrescription(String imageUrl, UUID prescriptionId);
}
