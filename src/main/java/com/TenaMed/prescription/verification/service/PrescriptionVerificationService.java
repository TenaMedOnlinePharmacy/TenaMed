package com.TenaMed.prescription.verification.service;

import com.TenaMed.Normalization.entity.PrescriptionItem;
import com.TenaMed.Normalization.repository.PrescriptionItemRepository;
import com.TenaMed.medicine.entity.Medicine;
import com.TenaMed.medicine.repository.MedicineRepository;
import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import com.TenaMed.prescription.verification.dto.PrescriptionItemRequestDto;
import com.TenaMed.prescription.verification.dto.VerificationDecision;
import com.TenaMed.prescription.verification.dto.VerificationResponseDto;
import com.TenaMed.prescription.verification.exception.PrescriptionNotFoundException;
import com.TenaMed.prescription.verification.exception.VerificationException;
import com.TenaMed.prescription.verification.event.PrescriptionVerifiedEvent;
import com.TenaMed.prescription.verification.workflow.VerificationEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PrescriptionVerificationService {

	private final PrescriptionRepository prescriptionRepository;
	private final PrescriptionItemRepository prescriptionItemRepository;
	private final MedicineRepository medicineRepository;
	private final VerificationEngine verificationEngine;
	private final ApplicationEventPublisher publisher;

	public PrescriptionVerificationService(PrescriptionRepository prescriptionRepository,
									   PrescriptionItemRepository prescriptionItemRepository,
									   MedicineRepository medicineRepository,
										   VerificationEngine verificationEngine,
										   ApplicationEventPublisher publisher) {
		this.prescriptionRepository = prescriptionRepository;
		this.prescriptionItemRepository = prescriptionItemRepository;
		this.medicineRepository = medicineRepository;
		this.verificationEngine = verificationEngine;
		this.publisher = publisher;
	}

	@Transactional
	public VerificationResponseDto verify(UUID prescriptionId, UUID requesterUserId) {
		Prescription prescription = prescriptionRepository.findById(prescriptionId)
				.orElseThrow(() -> new PrescriptionNotFoundException(prescriptionId));

		if (verificationEngine.isDigital(prescription.getType())) {
			prescriptionRepository.markVerified(prescriptionId, null, LocalDateTime.now());
			publisher.publishEvent(new PrescriptionVerifiedEvent(prescriptionId));
			return new VerificationResponseDto("VERIFIED", null, "ORDER_ALLOWED");
		}

		boolean ocrSuccess = Boolean.TRUE.equals(prescription.getOcrSuccess());
		double confidence = prescription.getConfidenceScore() == null ? 0.0 : prescription.getConfidenceScore();
		boolean highRisk = Boolean.TRUE.equals(prescription.getHighRisk());

		VerificationDecision decision = verificationEngine.evaluate(prescription.getType(), ocrSuccess, confidence, highRisk);

		if (decision.isRequiresManualReview()) {
			String reason = decision.getReviewReason() == null ? null : decision.getReviewReason().name();
			prescriptionRepository.markPendingManualReview(prescriptionId, reason);
			return new VerificationResponseDto("PENDING_MANUAL_REVIEW", reason, "WAIT_FOR_PHARMACIST");
		}

		if (decision.isVerified()) {
			prescriptionRepository.markVerified(prescriptionId, null, LocalDateTime.now());
			publisher.publishEvent(new PrescriptionVerifiedEvent(prescriptionId));
			return new VerificationResponseDto("VERIFIED", null, "ORDER_ALLOWED");
		}

		return new VerificationResponseDto("PENDING_MANUAL_REVIEW", null, "WAIT_FOR_PHARMACIST");
	}

	@Transactional
	public void validateAndSaveItems(UUID prescriptionId, List<PrescriptionItemRequestDto> items) {
		if (prescriptionId == null) {
			throw new VerificationException("prescriptionId is required");
		}
		if (items == null || items.isEmpty()) {
			throw new VerificationException("At least one prescription item is required");
		}

		Prescription prescription = prescriptionRepository.findById(prescriptionId)
				.orElseThrow(() -> new PrescriptionNotFoundException(prescriptionId));

		List<PrescriptionItem> toSave = items.stream().map(item -> {
			if (item.getMedicineId() == null) {
				throw new VerificationException("medicineId is required for each item");
			}
			if (item.getQuantity() == null || item.getQuantity() <= 0) {
				throw new VerificationException("quantity must be greater than zero for each item");
			}

			Medicine medicine = medicineRepository.findById(item.getMedicineId())
					.orElseThrow(() -> new VerificationException("Medicine not found: " + item.getMedicineId()));

			PrescriptionItem prescriptionItem = new PrescriptionItem();
			prescriptionItem.setPrescription(prescription);
			prescriptionItem.setMedicine(medicine);
			prescriptionItem.setQuantity(item.getQuantity());
			prescriptionItem.setStrength(item.getStrength());
			prescriptionItem.setForm(item.getForm());
			prescriptionItem.setInstructions(item.getInstructions());
			return prescriptionItem;
		}).toList();

		prescriptionItemRepository.deleteByPrescriptionId(prescriptionId);
		prescriptionItemRepository.saveAll(toSave);
		log.info("Prescription items validated and saved: prescriptionId={} itemCount={}", prescriptionId, toSave.size());
	}
}
