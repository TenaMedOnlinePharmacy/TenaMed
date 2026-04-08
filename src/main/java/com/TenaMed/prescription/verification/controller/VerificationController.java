package com.TenaMed.prescription.verification.controller;

import com.TenaMed.prescription.verification.dto.VerificationResponseDto;
import com.TenaMed.prescription.verification.service.ManualReviewService;
import com.TenaMed.prescription.verification.service.PrescriptionVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/verification")
public class VerificationController {

	private final PrescriptionVerificationService prescriptionVerificationService;
	private final ManualReviewService manualReviewService;

	public VerificationController(
			PrescriptionVerificationService prescriptionVerificationService,
			ManualReviewService manualReviewService
	) {
		this.prescriptionVerificationService = prescriptionVerificationService;
		this.manualReviewService = manualReviewService;
	}

	@PostMapping("/{id}/process")
	public VerificationResponseDto process(@PathVariable("id") UUID id) {
		return prescriptionVerificationService.verify(id);
	}

	@PostMapping("/{id}/approve")
	public ResponseEntity<Void> approve(
			@PathVariable("id") UUID id,
			@RequestParam UUID pharmacistId
	) {
		manualReviewService.approve(id, pharmacistId);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/{id}/reject")
	public ResponseEntity<Void> reject(
			@PathVariable("id") UUID id,
			@RequestParam UUID pharmacistId,
			@RequestParam String reason
	) {
		manualReviewService.reject(id, pharmacistId, reason);
		return ResponseEntity.ok().build();
	}
}
