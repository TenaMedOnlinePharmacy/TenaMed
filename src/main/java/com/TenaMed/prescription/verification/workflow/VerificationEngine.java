package com.TenaMed.prescription.verification.workflow;

import com.TenaMed.prescription.verification.dto.VerificationDecision;
import com.TenaMed.prescription.verification.enums.ReviewReason;
import org.springframework.stereotype.Component;

@Component
public class VerificationEngine {

	public boolean isDigital(String type) {
		return type != null && "DIGITAL".equalsIgnoreCase(type);
	}

	public boolean isOcrFailed(boolean ocrSuccess) {
		return !ocrSuccess;
	}

	public boolean isLowConfidence(double confidence) {
		return confidence < 0.8;
	}

	public boolean isHighRisk(boolean highRisk) {
		return highRisk;
	}

	public ReviewReason determineReviewReason(boolean ocrSuccess, double confidence, boolean highRisk) {
		if (isOcrFailed(ocrSuccess)) {
			return ReviewReason.OCR_FAILED;
		}

		if (isLowConfidence(confidence)) {
			return ReviewReason.LOW_CONFIDENCE;
		}

		if (isHighRisk(highRisk)) {
			return ReviewReason.HIGH_RISK;
		}

		return null;
	}

	public VerificationDecision evaluate(String type, boolean ocrSuccess, double confidence, boolean highRisk) {
		if (isDigital(type)) {
			return VerificationDecision.builder()
					.verified(true)
					.requiresManualReview(false)
					.reviewReason(null)
					.build();
		}

		ReviewReason reviewReason = determineReviewReason(ocrSuccess, confidence, highRisk);
		if (reviewReason != null) {
			return VerificationDecision.builder()
					.verified(false)
					.requiresManualReview(true)
					.reviewReason(reviewReason)
					.build();
		}

		return VerificationDecision.builder()
				.verified(true)
				.requiresManualReview(false)
				.reviewReason(null)
				.build();
	}
}
