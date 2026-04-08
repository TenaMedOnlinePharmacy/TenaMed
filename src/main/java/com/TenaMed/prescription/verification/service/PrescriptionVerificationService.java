package com.TenaMed.prescription.verification.service;

import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import com.TenaMed.prescription.verification.dto.VerificationDecision;
import com.TenaMed.prescription.verification.dto.VerificationResponseDto;
import com.TenaMed.prescription.verification.exception.PrescriptionNotFoundException;
import com.TenaMed.prescription.verification.event.PrescriptionVerifiedEvent;
import com.TenaMed.prescription.verification.workflow.VerificationEngine;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PrescriptionVerificationService {

	private final PrescriptionRepository prescriptionRepository;
	private final VerificationEngine verificationEngine;
	private final ApplicationEventPublisher publisher;

	public PrescriptionVerificationService(PrescriptionRepository prescriptionRepository,
										   VerificationEngine verificationEngine,
										   ApplicationEventPublisher publisher) {
		this.prescriptionRepository = prescriptionRepository;
		this.verificationEngine = verificationEngine;
		this.publisher = publisher;
	}

	@Transactional
	public VerificationResponseDto verify(UUID prescriptionId) {
		Prescription prescription = prescriptionRepository.findById(prescriptionId)
				.orElseThrow(() -> new PrescriptionNotFoundException(prescriptionId));

		if (verificationEngine.isDigital(prescription.getType())) {
			prescription.setStatus("VERIFIED");
			prescription.setVerifiedAt(LocalDateTime.now());
			prescriptionRepository.save(prescription);
			publisher.publishEvent(new PrescriptionVerifiedEvent(prescriptionId));
			return new VerificationResponseDto("VERIFIED", null, "ORDER_ALLOWED");
		}

		boolean ocrSuccess = Boolean.TRUE.equals(prescription.getOcrSuccess());
		double confidence = prescription.getConfidenceScore() == null ? 0.0 : prescription.getConfidenceScore();
		boolean highRisk = Boolean.TRUE.equals(prescription.getHighRisk());

		VerificationDecision decision = verificationEngine.evaluate(prescription.getType(), ocrSuccess, confidence, highRisk);

		if (decision.isRequiresManualReview()) {
			String reason = decision.getReviewReason() == null ? null : decision.getReviewReason().name();
			prescription.setStatus("PENDING_MANUAL_REVIEW");
			prescription.setReviewReason(reason);
			prescriptionRepository.save(prescription);
			return new VerificationResponseDto("PENDING_MANUAL_REVIEW", reason, "WAIT_FOR_PHARMACIST");
		}

		if (decision.isVerified()) {
			prescription.setStatus("VERIFIED");
			prescription.setVerifiedAt(LocalDateTime.now());
			prescription.setVerifiedBy(null);
			prescriptionRepository.save(prescription);
			publisher.publishEvent(new PrescriptionVerifiedEvent(prescriptionId));
			return new VerificationResponseDto("VERIFIED", null, "ORDER_ALLOWED");
		}

		return new VerificationResponseDto("PENDING_MANUAL_REVIEW", null, "WAIT_FOR_PHARMACIST");
	}
}
