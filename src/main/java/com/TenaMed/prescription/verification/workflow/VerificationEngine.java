package com.TenaMed.prescription.verification.workflow;

import com.TenaMed.prescription.verification.enums.ReviewReason;

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
}
