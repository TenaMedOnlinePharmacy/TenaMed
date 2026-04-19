package com.TenaMed.verification.service;

import com.TenaMed.Normalization.model.NormalizedOcrMedicineItem;
import com.TenaMed.Normalization.model.NormalizedOcrResultDto;
import com.TenaMed.Normalization.entity.PrescriptionItem;
import com.TenaMed.Normalization.repository.PrescriptionItemRepository;
import com.TenaMed.medicine.entity.Medicine;
import com.TenaMed.medicine.repository.MedicineRepository;
import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import com.TenaMed.verification.dto.PrescriptionItemRequestDto;
import com.TenaMed.verification.dto.VerificationDecision;
import com.TenaMed.verification.dto.VerificationResponseDto;
import com.TenaMed.verification.exception.PrescriptionNotFoundException;
import com.TenaMed.verification.exception.VerificationException;
import com.TenaMed.verification.event.PrescriptionVerifiedEvent;
import com.TenaMed.verification.workflow.VerificationEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

	@Value("${pipeline.normalized-confidence-threshold}")
	private double normalizedConfidenceThreshold;

	public record NormalizedResultCheck(boolean valid, String reason) {}

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
	public VerificationResponseDto verify(UUID prescriptionId) {
		Prescription prescription = prescriptionRepository.findById(prescriptionId)
				.orElseThrow(() -> new PrescriptionNotFoundException(prescriptionId));
		String oldStatus = prescription.getStatus();

		if (verificationEngine.isDigital(prescription.getType())) {
			prescriptionRepository.markVerified(prescriptionId, null, LocalDateTime.now());
			publisher.publishEvent(new PrescriptionVerifiedEvent(prescriptionId, oldStatus, "VERIFIED", "SYSTEM", null));
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
			publisher.publishEvent(new PrescriptionVerifiedEvent(prescriptionId, oldStatus, "VERIFIED", "SYSTEM", null));
			return new VerificationResponseDto("VERIFIED", null, "ORDER_ALLOWED");
		}

		return new VerificationResponseDto("PENDING_MANUAL_REVIEW", null, "WAIT_FOR_PHARMACIST");
	}

	@Transactional
	public void validateAndSaveItems(UUID prescriptionId, UUID verifiedBy, List<PrescriptionItemRequestDto> items) {
		if (prescriptionId == null) {
			throw new VerificationException("prescriptionId is required");
		}
		if (verifiedBy == null) {
			throw new VerificationException("verifiedBy is required");
		}
		if (items == null || items.isEmpty()) {
			throw new VerificationException("At least one prescription item is required");
		}

		Prescription prescription = prescriptionRepository.findById(prescriptionId)
				.orElseThrow(() -> new PrescriptionNotFoundException(prescriptionId));
		String oldStatus = prescription.getStatus();

		List<PrescriptionItem> toSave = items.stream().map(item -> {
			if (item.getMedicineName() == null || item.getMedicineName().isBlank()) {
				throw new VerificationException("medicineName is required for each item");
			}
			if (item.getQuantity() == null || item.getQuantity() <= 0) {
				throw new VerificationException("quantity must be greater than zero for each item");
			}

			String medicineName = item.getMedicineName().trim();

			Medicine medicine = medicineRepository
					.findFirstByNameIgnoreCaseOrGenericNameIgnoreCase(medicineName, medicineName)
					.orElseThrow(() -> new VerificationException("Medicine not found by name or generic name: " + medicineName));

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

		int updatedRows = prescriptionRepository.markVerifiedPreserveReviewReason(prescriptionId, verifiedBy, LocalDateTime.now());
		if (updatedRows != 1) {
			throw new VerificationException("Failed to update prescription verification status: " + prescriptionId);
		}

		publisher.publishEvent(new PrescriptionVerifiedEvent(prescriptionId, oldStatus, "VERIFIED", "PHARMACIST", verifiedBy));

		log.info("Prescription items validated and saved: prescriptionId={} itemCount={}", prescriptionId, toSave.size());
	}

	public NormalizedResultCheck checkNormalizedResult(NormalizedOcrResultDto normalizedResult) {
		if (normalizedResult == null || normalizedResult.getMedicines() == null || normalizedResult.getMedicines().isEmpty()) {
			return new NormalizedResultCheck(false, "LOW_CONFIDENCE");
		}

		for (NormalizedOcrMedicineItem item : normalizedResult.getMedicines()) {
			if (item == null) {
				return new NormalizedResultCheck(false, "UNKNOWN_MEDICINE");
			}

			if (item.getConfidence() <= normalizedConfidenceThreshold) {
				return new NormalizedResultCheck(false, "LOW_CONFIDENCE");
			}

			if (item.isNeedsReview()) {
				return new NormalizedResultCheck(false, "UNKNOWN_MEDICINE");
			}
		}

		return new NormalizedResultCheck(true, null);
	}
}
