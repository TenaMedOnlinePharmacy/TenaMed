package com.TenaMed.pharmacy.controller;

import com.TenaMed.medicine.dto.MedicinePharmacySearchResponseDto;
import com.TenaMed.pharmacy.exception.PharmacyException;
import com.TenaMed.pharmacy.service.PrescriptionInventoryMatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/pharmacy/prescriptions")
public class PrescriptionInventoryController {

    private final PrescriptionInventoryMatchService prescriptionInventoryMatchService;

    public PrescriptionInventoryController(PrescriptionInventoryMatchService prescriptionInventoryMatchService) {
        this.prescriptionInventoryMatchService = prescriptionInventoryMatchService;
    }

    @GetMapping("/{prescriptionId}/inventory-matches")
    public ResponseEntity<?> getInventoryMatches(@PathVariable UUID prescriptionId) {
        try {
            List<MedicinePharmacySearchResponseDto> matches =
                prescriptionInventoryMatchService.findInventoryMatchesByPrescription(prescriptionId);
            return ResponseEntity.ok(matches);
        } catch (PharmacyException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/items/{prescriptionItemId}/product-options")
    public ResponseEntity<?> getProductOptions(@PathVariable UUID prescriptionItemId) {
        try {
            List<com.TenaMed.pharmacy.dto.response.PrescriptionProductOptionDto> options =
                prescriptionInventoryMatchService.getProductOptionsForPrescriptionItem(prescriptionItemId);
            return ResponseEntity.ok(options);
        } catch (PharmacyException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
        }
    }
}