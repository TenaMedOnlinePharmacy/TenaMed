package com.TenaMed.verification.event;

import java.util.UUID;

public class PrescriptionRejectedEvent {
	private final UUID prescriptionId;
	private final String reason;
	private final String oldStatus;
	private final String newStatus;
	private final String actorType;
	private final UUID actorId;

	public PrescriptionRejectedEvent(UUID prescriptionId, String reason) {
		this(prescriptionId, reason, null, "REJECTED", null, null);
	}

	public PrescriptionRejectedEvent(UUID prescriptionId,
								 String reason,
								 String oldStatus,
								 String newStatus,
								 String actorType,
								 UUID actorId) {
		this.prescriptionId = prescriptionId;
		this.reason = reason;
		this.oldStatus = oldStatus;
		this.newStatus = newStatus;
		this.actorType = actorType;
		this.actorId = actorId;
	}

	public UUID getPrescriptionId() {
		return prescriptionId;
	}

	public String getReason() {
		return reason;
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
