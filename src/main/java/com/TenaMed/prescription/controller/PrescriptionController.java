package com.TenaMed.prescription.controller;

import com.TenaMed.prescription.dto.PrescriptionResponseDto;
import com.TenaMed.prescription.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @GetMapping("/hospital-issued")
    public ResponseEntity<PrescriptionResponseDto> getPrescriptionDetails(
            @RequestParam String uniqueCode,
            @RequestParam String phone) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionDetails(uniqueCode, phone));
    }
}
