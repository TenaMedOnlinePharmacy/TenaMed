package com.TenaMed.prescription.verification.event;

import java.util.UUID;

public class PrescriptionRejectedEvent {
	private final UUID prescriptionId;
	private final String reason;

	public PrescriptionRejectedEvent(UUID prescriptionId, String reason) {
		this.prescriptionId = prescriptionId;
		this.reason = reason;
	}

	public UUID getPrescriptionId() {
		return prescriptionId;
	}

	public String getReason() {
		return reason;
	}
}
