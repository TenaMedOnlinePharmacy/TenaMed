package com.TenaMed.ocr.integration;

import com.TenaMed.ocr.dto.OcrResultDto;

public interface OcrClient {
    OcrResultDto processPrescription(String imageUrl);
}
