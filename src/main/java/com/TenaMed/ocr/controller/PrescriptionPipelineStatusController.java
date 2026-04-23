package com.TenaMed.ocr.controller;

import com.TenaMed.medicine.dto.MedicinePharmacySearchResponseDto;
import com.TenaMed.ocr.dto.response.PrescriptionPipelineStatusResponseDto;
import com.TenaMed.pharmacy.service.PrescriptionInventoryMatchService;
import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ocr/pipeline")
@RequiredArgsConstructor
public class PrescriptionPipelineStatusController {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionInventoryMatchService prescriptionInventoryMatchService;

    @GetMapping("/{prescriptionId}/status")
        public ResponseEntity<PrescriptionPipelineStatusResponseDto> getPipelineStatus(@PathVariable UUID prescriptionId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId).orElse(null);
        if (prescription == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new PrescriptionPipelineStatusResponseDto(
                    "NOT_FOUND",
                    "Prescription not found",
                    prescriptionId,
                    null,
                    null
                ));
        }

        String prescriptionStatus = prescription.getStatus() == null ? "UPLOADED" : prescription.getStatus();

        if ("VERIFIED".equals(prescriptionStatus)) {
            List<MedicinePharmacySearchResponseDto> matches =
                    prescriptionInventoryMatchService.findInventoryMatchesByPrescription(prescriptionId);
            return ResponseEntity.ok(new PrescriptionPipelineStatusResponseDto(
                "FINISHED",
                "Pipeline finished successfully",
                prescriptionId,
                prescriptionStatus,
                matches
            ));
        }

        if ("FAILED".equals(prescriptionStatus)) {
            return ResponseEntity.ok(new PrescriptionPipelineStatusResponseDto(
                "FAILED",
                prescription.getRejectionReason() == null ? "Pipeline failed" : prescription.getRejectionReason(),
                prescriptionId,
                prescriptionStatus,
                null
            ));
        }

        if ("PENDING_MANUAL_REVIEW".equals(prescriptionStatus)) {
            return ResponseEntity.ok(new PrescriptionPipelineStatusResponseDto(
                "PENDING_MANUAL_REVIEW",
                "Pipeline requires manual review",
                prescriptionId,
                prescriptionStatus,
                null
            ));
        }

        return ResponseEntity.ok(new PrescriptionPipelineStatusResponseDto(
            "PROCESSING",
            "Pipeline is still processing",
            prescriptionId,
            prescriptionStatus,
            null
        ));
    }
}
