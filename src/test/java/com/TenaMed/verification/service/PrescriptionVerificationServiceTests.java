package com.TenaMed.verification.service;

import com.TenaMed.Normalization.model.MatchType;
import com.TenaMed.Normalization.model.NormalizedOcrMedicineItem;
import com.TenaMed.Normalization.model.NormalizedOcrResultDto;
import com.TenaMed.Normalization.repository.PrescriptionItemRepository;
import com.TenaMed.medicine.repository.MedicineRepository;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import com.TenaMed.verification.workflow.VerificationEngine;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PrescriptionVerificationServiceTests {

    @Test
    void checkNormalizedResultShouldFailWhenAnyMedicineNeedsReviewOrLowConfidence() {
        PrescriptionVerificationService service = new PrescriptionVerificationService(
                mock(PrescriptionRepository.class),
                mock(PrescriptionItemRepository.class),
                mock(MedicineRepository.class),
                mock(VerificationEngine.class),
                event -> {
                }
        );
        ReflectionTestUtils.setField(service, "normalizedConfidenceThreshold", 0.6);

        NormalizedOcrResultDto normalized = new NormalizedOcrResultDto(true, 0.8, List.of(
                new NormalizedOcrMedicineItem("A", "A", MatchType.EXACT, 0.7, 0.8, false, 1, "x"),
                new NormalizedOcrMedicineItem("B", "B", MatchType.EXACT, 0.59, 0.8, false, 1, "x")
        ));

        PrescriptionVerificationService.NormalizedResultCheck check = service.checkNormalizedResult(normalized);

        assertFalse(check.valid());
    }

    @Test
    void checkNormalizedResultShouldPassWhenAllMedicinesAreStrongAndNoReviewNeeded() {
        PrescriptionVerificationService service = new PrescriptionVerificationService(
                mock(PrescriptionRepository.class),
                mock(PrescriptionItemRepository.class),
                mock(MedicineRepository.class),
                mock(VerificationEngine.class),
                event -> {
                }
        );
        ReflectionTestUtils.setField(service, "normalizedConfidenceThreshold", 0.6);

        NormalizedOcrResultDto normalized = new NormalizedOcrResultDto(true, 0.9, List.of(
                new NormalizedOcrMedicineItem("A", "A", MatchType.EXACT, 0.9, 0.9, false, 1, "x")
        ));

        PrescriptionVerificationService.NormalizedResultCheck check = service.checkNormalizedResult(normalized);

        assertTrue(check.valid());
    }
}
