package com.TenaMed.prescription.verification.service;

import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import com.TenaMed.prescription.verification.exception.InvalidVerificationStateException;
import com.TenaMed.prescription.verification.exception.PrescriptionNotFoundException;
import com.TenaMed.prescription.verification.event.PrescriptionRejectedEvent;
import com.TenaMed.prescription.verification.event.PrescriptionVerifiedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ManualReviewService {

	private final PrescriptionRepository prescriptionRepository;
	private final ApplicationEventPublisher publisher;

	public ManualReviewService(PrescriptionRepository prescriptionRepository, ApplicationEventPublisher publisher) {
		this.prescriptionRepository = prescriptionRepository;
		this.publisher = publisher;
	}

	@Transactional
	public void approve(UUID prescriptionId, UUID pharmacistId) {
		Prescription prescription = prescriptionRepository.findById(prescriptionId)
				.orElseThrow(() -> new PrescriptionNotFoundException(prescriptionId));

		if (!"PENDING_MANUAL_REVIEW".equals(prescription.getStatus())) {
			throw new InvalidVerificationStateException("Prescription is not pending manual review");
		}

		prescriptionRepository.markVerified(prescriptionId, pharmacistId, LocalDateTime.now());
		publisher.publishEvent(new PrescriptionVerifiedEvent(prescriptionId));
	}

	@Transactional
	public void reject(UUID prescriptionId, UUID pharmacistId, String reason) {
		Prescription prescription = prescriptionRepository.findById(prescriptionId)
				.orElseThrow(() -> new PrescriptionNotFoundException(prescriptionId));

		if (!"PENDING_MANUAL_REVIEW".equals(prescription.getStatus())) {
			throw new InvalidVerificationStateException("Prescription is not pending manual review");
		}

		prescriptionRepository.markRejected(prescriptionId, reason, pharmacistId);
		publisher.publishEvent(new PrescriptionRejectedEvent(prescriptionId, reason));
	}
}
