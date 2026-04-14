package com.TenaMed.verification.workflow;

import com.TenaMed.verification.dto.VerificationDecision;
import com.TenaMed.prescription.entity.PrescriptionType;
import com.TenaMed.verification.enums.ReviewReason;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VerificationEngine {

	public boolean isDigital(PrescriptionType type) {
		return type == PrescriptionType.DIGITAL;
	}

	public boolean isOcrFailed(boolean ocrSuccess) {
		return !ocrSuccess;
	}

	@Value("${ocr.confidence-threshold}")
	private int ocrConfidenceThreshold;

	public boolean isLowConfidence(double confidence) {
		return confidence < ocrConfidenceThreshold;
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

	public VerificationDecision evaluate(PrescriptionType type, boolean ocrSuccess, double confidence, boolean highRisk) {
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
