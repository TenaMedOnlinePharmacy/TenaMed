package com.TenaMed.verification.enums;

// Captures why a prescription requires manual review or rejection.
public enum ReviewReason {
	OCR_FAILED,
	LOW_CONFIDENCE,
	HIGH_RISK
}
