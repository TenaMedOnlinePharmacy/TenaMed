package com.TenaMed.ocr.dto.response;

import java.util.UUID;

public record OcrUploadResponseDto(
        String status,
        String message,
        UUID prescriptionId
) {
}
