package com.TenaMed.ocr.service;

import com.TenaMed.Normalization.model.MatchType;
import com.TenaMed.Normalization.model.NormalizedOcrMedicineItem;
import com.TenaMed.Normalization.model.NormalizedOcrResultDto;
import com.TenaMed.Normalization.service.OcrDrugNormalizationService;
import com.TenaMed.manualreview.entity.TaskPriority;
import com.TenaMed.manualreview.entity.TaskReason;
import com.TenaMed.manualreview.service.ManualReviewService;
import com.TenaMed.ocr.dto.OcrResultDto;
import com.TenaMed.ocr.integration.OcrClient;
import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import com.TenaMed.events.DomainEventService;
import com.TenaMed.verification.dto.VerificationResponseDto;
import com.TenaMed.verification.service.PrescriptionVerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrescriptionPipelineServiceTests {

    @Test
    void shouldCreateHighPriorityTaskWhenVerificationReturnsPending() {
        OcrClient ocrClient = mock(OcrClient.class);
        PrescriptionVerificationService verificationService = mock(PrescriptionVerificationService.class);
        OcrDrugNormalizationService normalizationService = mock(OcrDrugNormalizationService.class);
        ManualReviewService manualReviewService = mock(ManualReviewService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
        DomainEventService domainEventService = mock(DomainEventService.class);

        PrescriptionPipelineService service = new PrescriptionPipelineService(
                ocrClient,
                verificationService,
                normalizationService,
                manualReviewService,
                publisher,
                prescriptionRepository,
                domainEventService
        );

        UUID prescriptionId = UUID.randomUUID();
        Prescription prescription = new Prescription();
        prescription.setId(prescriptionId);
        when(ocrClient.processPrescription("url", prescriptionId)).thenReturn(new OcrResultDto(true, 0.9, List.of(), prescription));
        when(verificationService.verify(prescriptionId))
                .thenReturn(new VerificationResponseDto("PENDING_MANUAL_REVIEW", "LOW_CONFIDENCE", "WAIT_FOR_PHARMACIST"));

        service.processPipeline("url", prescriptionId);

        verify(manualReviewService).createTask(prescriptionId, TaskReason.LOW_CONFIDENCE, TaskPriority.HIGH);
        verify(normalizationService, never()).normalize(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldPersistAndPublishEventWhenFullySuccessful() {
        OcrClient ocrClient = mock(OcrClient.class);
        PrescriptionVerificationService verificationService = mock(PrescriptionVerificationService.class);
        OcrDrugNormalizationService normalizationService = mock(OcrDrugNormalizationService.class);
        ManualReviewService manualReviewService = mock(ManualReviewService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
        DomainEventService domainEventService = mock(DomainEventService.class);

        PrescriptionPipelineService service = new PrescriptionPipelineService(
                ocrClient,
                verificationService,
                normalizationService,
                manualReviewService,
                publisher,
                prescriptionRepository,
                domainEventService
        );

        UUID prescriptionId = UUID.randomUUID();
        Prescription prescription = new Prescription();
        prescription.setId(prescriptionId);
        OcrResultDto ocrResult = new OcrResultDto(true, 0.9, List.of(), prescription);
        NormalizedOcrResultDto normalized = new NormalizedOcrResultDto(true, 0.9, List.of(
                new NormalizedOcrMedicineItem("A", "A", MatchType.EXACT, 0.9, 0.9, false, 1, "x")
        ));

        when(ocrClient.processPrescription("url", prescriptionId)).thenReturn(ocrResult);
        when(verificationService.verify(prescriptionId))
                .thenReturn(new VerificationResponseDto("VERIFIED", null, "ORDER_ALLOWED"));
        when(normalizationService.normalize(ocrResult)).thenReturn(normalized);
        when(verificationService.checkNormalizedResult(normalized))
                .thenReturn(new PrescriptionVerificationService.NormalizedResultCheck(true, null));

        service.processPipeline("url", prescriptionId);

        verify(normalizationService).persistPrescriptionOutcome(ocrResult, normalized);
        verify(publisher).publishEvent(org.mockito.ArgumentMatchers.any(com.TenaMed.ocr.event.PrescriptionPipelinePersistedEvent.class));
        verify(manualReviewService, never()).createTask(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
