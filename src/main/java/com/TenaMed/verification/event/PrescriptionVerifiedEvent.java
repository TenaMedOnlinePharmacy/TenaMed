package com.TenaMed.verification.event;

import java.util.UUID;

public class PrescriptionVerifiedEvent {
	private final UUID prescriptionId;

	public PrescriptionVerifiedEvent(UUID prescriptionId) {
		this.prescriptionId = prescriptionId;
	}

	public UUID getPrescriptionId() {
		return prescriptionId;
	}
}
