package com.TenaMed.ocr.dto.response;

import com.TenaMed.pharmacy.dto.response.PrescriptionInventoryMatchDto;

import java.util.List;
import java.util.UUID;

public record PrescriptionPipelineStatusResponseDto(
        String status,
        String message,
        UUID prescriptionId,
        String prescriptionStatus,
        List<PrescriptionInventoryMatchDto> inventoryMatches
) {
}
