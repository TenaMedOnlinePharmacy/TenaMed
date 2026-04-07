package com.TenaMed.prescription.verification.service;

import java.util.UUID;

public interface ManualReviewService {
	void approve(UUID prescriptionId, UUID pharmacistId);

	void reject(UUID prescriptionId, UUID pharmacistId, String reason);
}
