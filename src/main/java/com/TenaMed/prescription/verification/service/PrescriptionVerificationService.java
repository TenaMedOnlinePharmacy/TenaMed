package com.TenaMed.prescription.verification.service;

import com.TenaMed.prescription.verification.dto.VerificationDecision;
import com.TenaMed.prescription.verification.dto.VerificationResponseDto;
import com.TenaMed.prescription.verification.workflow.VerificationEngine;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PrescriptionVerificationService {

	private final VerificationEngine verificationEngine;

	public PrescriptionVerificationService(VerificationEngine verificationEngine) {
		this.verificationEngine = verificationEngine;
	}

	public VerificationResponseDto verify(UUID prescriptionId) {
		String type = "UPLOADED";
		boolean ocrSuccess = true;
		double confidence = 0.9;
		boolean highRisk = false;

		VerificationDecision decision = verificationEngine.evaluate(type, ocrSuccess, confidence, highRisk);

		if (decision.isVerified()) {
			return new VerificationResponseDto("VERIFIED", null, "ORDER_ALLOWED");
		}

		if (decision.isRequiresManualReview()) {
			String reason = decision.getReviewReason() == null ? null : decision.getReviewReason().name();
			return new VerificationResponseDto("PENDING_MANUAL_REVIEW", reason, "WAIT_FOR_PHARMACIST");
		}

		return new VerificationResponseDto("PENDING_MANUAL_REVIEW", null, "WAIT_FOR_PHARMACIST");
	}
}
