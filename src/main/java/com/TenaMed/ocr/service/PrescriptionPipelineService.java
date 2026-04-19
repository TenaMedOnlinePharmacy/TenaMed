package com.TenaMed.ocr.service;

import com.TenaMed.Normalization.model.NormalizedOcrResultDto;
import com.TenaMed.Normalization.service.OcrDrugNormalizationService;
import com.TenaMed.manualreview.entity.TaskPriority;
import com.TenaMed.manualreview.entity.TaskReason;
import com.TenaMed.manualreview.service.ManualReviewService;
import com.TenaMed.ocr.dto.OcrResultDto;
import com.TenaMed.ocr.event.PrescriptionPipelinePersistedEvent;
import com.TenaMed.ocr.integration.OcrClient;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import com.TenaMed.verification.dto.VerificationResponseDto;
import com.TenaMed.verification.service.PrescriptionVerificationService;
import com.TenaMed.events.DomainEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionPipelineService {

    private final OcrClient ocrClient;
    private final PrescriptionVerificationService prescriptionVerificationService;
    private final OcrDrugNormalizationService ocrDrugNormalizationService;
    private final ManualReviewService manualReviewService;
    private final ApplicationEventPublisher eventPublisher;
    private final PrescriptionRepository prescriptionRepository;
    private final DomainEventService domainEventService;

    @Async
    public void startPipeline(String imageUrl, UUID prescriptionId) {
        try {
            processPipeline(imageUrl, prescriptionId);
        } catch (Exception ex) {
            markFailed(prescriptionId, ex.getMessage());
            log.error("Prescription pipeline failed: prescriptionId={} reason={}", prescriptionId, ex.getMessage(), ex);
        }
    }

    // Separate method makes synchronous unit testing straightforward.
    void processPipeline(String imageUrl, UUID prescriptionId) {
        prescriptionRepository.markProcessing(prescriptionId);
        domainEventService.publish(
            "PRESCRIPTION_PIPELINE_STARTED",
            "PRESCRIPTION",
            prescriptionId,
            "SYSTEM",
            null,
            "PLATFORM",
            null,
            Map.of()
        );

        OcrResultDto ocrResult = ocrClient.processPrescription(imageUrl, prescriptionId);
        if (ocrResult == null) {
            throw new IllegalStateException("OCR returned null result");
        }

        prescriptionRepository.updateOcrOutcomeById(
                prescriptionId,
                ocrResult.isSuccess(),
                ocrResult.getConfidence()
        );

        VerificationResponseDto verificationResponse = prescriptionVerificationService.verify(prescriptionId);
        if (verificationResponse == null) {
            throw new IllegalStateException("Verification returned null response");
        }

        if (isPendingManualReview(verificationResponse)) {
            createHighPriorityTask(prescriptionId, mapTaskReasonFromVerification(verificationResponse.getReason()));
            return;
        }

        if (!"VERIFIED".equals(verificationResponse.getStatus())) {
            markFailed(prescriptionId, "Unsupported verification status: " + verificationResponse.getStatus());
            log.warn("Pipeline stopped due to unsupported verification status: prescriptionId={} status={}",
                    prescriptionId, verificationResponse.getStatus());
            return;
        }

        NormalizedOcrResultDto normalizedResult = ocrDrugNormalizationService.normalize(ocrResult);
        PrescriptionVerificationService.NormalizedResultCheck normalizedCheck =
                prescriptionVerificationService.checkNormalizedResult(normalizedResult);
        if (!normalizedCheck.valid()) {
            prescriptionRepository.markPendingManualReview(prescriptionId, normalizedCheck.reason());
            createHighPriorityTask(prescriptionId, mapTaskReasonFromNormalizedCheck(normalizedCheck.reason()));
            return;
        }

        ocrDrugNormalizationService.persistPrescriptionOutcome(ocrResult, normalizedResult);

        int medicinesCount = normalizedResult.getMedicines() == null ? 0 : normalizedResult.getMedicines().size();
        domainEventService.publish(
            "OCR_PROCESSED",
            "PRESCRIPTION",
            prescriptionId,
            "SYSTEM",
            null,
            "PLATFORM",
            null,
            Map.of("medicinesCount", medicinesCount)
        );
        eventPublisher.publishEvent(new PrescriptionPipelinePersistedEvent(prescriptionId, medicinesCount));
        log.info("Prescription pipeline completed successfully: prescriptionId={} medicinesCount={}",
                prescriptionId, medicinesCount);
    }

    private boolean isPendingManualReview(VerificationResponseDto response) {
        return response != null && "PENDING_MANUAL_REVIEW".equals(response.getStatus());
    }

    private void createHighPriorityTask(UUID prescriptionId, TaskReason reason) {
        manualReviewService.createTask(prescriptionId, reason, TaskPriority.HIGH);
        log.info("High-priority manual review task created: prescriptionId={} reason={}", prescriptionId, reason);
    }

    private TaskReason mapTaskReasonFromVerification(String reason) {
        if ("OCR_FAILED".equals(reason)) {
            return TaskReason.OCR_FAILED;
        }

        if ("LOW_CONFIDENCE".equals(reason)) {
            return TaskReason.LOW_CONFIDENCE;
        }

        return TaskReason.UNKNOWN_MEDICINE;
    }

    private TaskReason mapTaskReasonFromNormalizedCheck(String reason) {
        if ("LOW_CONFIDENCE".equals(reason)) {
            return TaskReason.LOW_CONFIDENCE;
        }

        return TaskReason.UNKNOWN_MEDICINE;
    }

    private void markFailed(UUID prescriptionId, String reason) {
        String errorMessage = reason == null || reason.isBlank() ? "Pipeline failed" : reason;
        if (errorMessage.length() > 500) {
            errorMessage = errorMessage.substring(0, 500);
        }
        prescriptionRepository.markFailed(prescriptionId, errorMessage);
        domainEventService.publish(
                "PRESCRIPTION_PIPELINE_FAILED",
                "PRESCRIPTION",
                prescriptionId,
                "SYSTEM",
                null,
                "PLATFORM",
                null,
                Map.of("reason", errorMessage)
        );
    }
}
