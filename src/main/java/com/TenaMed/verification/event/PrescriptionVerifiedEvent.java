package com.TenaMed.verification.event;

import java.util.UUID;

public class PrescriptionVerifiedEvent {
	private final UUID prescriptionId;
	private final String oldStatus;
	private final String newStatus;
	private final String actorType;
	private final UUID actorId;

	public PrescriptionVerifiedEvent(UUID prescriptionId) {
		this(prescriptionId, null, "VERIFIED", null, null);
	}

	public PrescriptionVerifiedEvent(UUID prescriptionId,
								 String oldStatus,
								 String newStatus,
								 String actorType,
								 UUID actorId) {
		this.prescriptionId = prescriptionId;
		this.oldStatus = oldStatus;
		this.newStatus = newStatus;
		this.actorType = actorType;
		this.actorId = actorId;
	}

	public UUID getPrescriptionId() {
		return prescriptionId;
	}

	public String getOldStatus() {
		return oldStatus;
	}

	public String getNewStatus() {
		return newStatus;
	}

	public String getActorType() {
		return actorType;
	}

	public UUID getActorId() {
		return actorId;
	}
}
