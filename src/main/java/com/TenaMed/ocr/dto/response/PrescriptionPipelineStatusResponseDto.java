package com.TenaMed.ocr.dto.response;

import com.TenaMed.medicine.dto.MedicinePharmacySearchResponseDto;

import java.util.List;
import java.util.UUID;

public record PrescriptionPipelineStatusResponseDto(
        String status,
        String message,
        UUID prescriptionId,
        String prescriptionStatus,
        List<MedicinePharmacySearchResponseDto> inventoryMatches
) {
}
