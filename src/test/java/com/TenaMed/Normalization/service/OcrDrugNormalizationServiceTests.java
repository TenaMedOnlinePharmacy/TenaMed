package com.TenaMed.Normalization.service;

import com.TenaMed.Normalization.model.MatchType;
import com.TenaMed.Normalization.model.NormalizedOcrResultDto;
import com.TenaMed.Normalization.repository.PrescriptionItemRepository;
import com.TenaMed.medicine.entity.Medicine;
import com.TenaMed.medicine.repository.MedicineRepository;
import com.TenaMed.ocr.dto.MedicineOcrItem;
import com.TenaMed.ocr.dto.OcrResultDto;
import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OcrDrugNormalizationServiceTests {

    @Test
    void shouldNormalizeWithoutPersisting() {
        DrugNormalizationService normalizationService = new DrugNormalizationService(defaultLookup(), 0.90, 0.02);
        PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
        PrescriptionItemRepository prescriptionItemRepository = mock(PrescriptionItemRepository.class);
        MedicineRepository medicineRepository = mock(MedicineRepository.class);

        OcrDrugNormalizationService service = new OcrDrugNormalizationService(
                normalizationService,
                prescriptionRepository,
                prescriptionItemRepository,
                medicineRepository
        );

        Medicine paracetamol = new Medicine();
        paracetamol.setName("Paracetamol");
        when(medicineRepository.findByNameIgnoreCase("Tylenol")).thenReturn(Optional.of(paracetamol));

        OcrResultDto ocrResult = new OcrResultDto(
                true,
                0.698,
                List.of(
                        new MedicineOcrItem("Tylenol", null, "tat #30; Sig A.D."),
                        new MedicineOcrItem("zzzzzz", null, "#30; Sig. Once a day")
                ),
                null
        );

        NormalizedOcrResultDto result = service.normalize(ocrResult);

        assertTrue(result.isSuccess());
        assertEquals(0.698, result.getConfidence());
        assertEquals(2, result.getMedicines().size());

        assertEquals("Tylenol", result.getMedicines().get(0).getOriginalName());
        assertEquals("Paracetamol", result.getMedicines().get(0).getNormalizedName());
        assertEquals(MatchType.SYNONYM, result.getMedicines().get(0).getMatchType());
        assertFalse(result.getMedicines().get(0).isNeedsReview());

        assertEquals("zzzzzz", result.getMedicines().get(1).getOriginalName());
        assertEquals(null, result.getMedicines().get(1).getNormalizedName());
        assertEquals(MatchType.UNKNOWN, result.getMedicines().get(1).getMatchType());
        assertTrue(result.getMedicines().get(1).isNeedsReview());

        verify(prescriptionRepository, never()).updateOcrOutcomeById(any(), any(), any());
        verify(prescriptionItemRepository, never()).deleteByPrescriptionId(any());
        verify(prescriptionItemRepository, never()).save(any());
    }

    @Test
    void shouldPersistOutcomeAndResolvedItems() {
        DrugNormalizationService normalizationService = new DrugNormalizationService(defaultLookup(), 0.90, 0.02);
        PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
        PrescriptionItemRepository prescriptionItemRepository = mock(PrescriptionItemRepository.class);
        MedicineRepository medicineRepository = mock(MedicineRepository.class);

        OcrDrugNormalizationService service = new OcrDrugNormalizationService(
                normalizationService,
                prescriptionRepository,
                prescriptionItemRepository,
                medicineRepository
        );

        UUID prescriptionId = UUID.randomUUID();
        Prescription prescription = new Prescription();
        prescription.setId(prescriptionId);

        Medicine matchedMedicine = new Medicine();
        matchedMedicine.setName("Paracetamol");
        when(medicineRepository.findByNameIgnoreCase("Paracetamol")).thenReturn(Optional.of(matchedMedicine));

        OcrResultDto ocrResult = new OcrResultDto(true, 0.60, List.of(), prescription);
        NormalizedOcrResultDto normalizedResult = new NormalizedOcrResultDto(
                true,
                0.60,
                List.of(new com.TenaMed.Normalization.model.NormalizedOcrMedicineItem(
                        "Tylenol",
                        "Paracetamol",
                        MatchType.SYNONYM,
                        0.90,
                        0.60,
                        false,
                        30,
                        "Take after meal"
                ))
        );

        service.persistPrescriptionOutcome(ocrResult, normalizedResult);

        verify(prescriptionRepository).updateOcrOutcomeById(eq(prescriptionId), eq(true), eq(0.75));
        verify(prescriptionItemRepository).deleteByPrescriptionId(prescriptionId);

        ArgumentCaptor<com.TenaMed.Normalization.entity.PrescriptionItem> captor =
                ArgumentCaptor.forClass(com.TenaMed.Normalization.entity.PrescriptionItem.class);
        verify(prescriptionItemRepository).save(captor.capture());

        com.TenaMed.Normalization.entity.PrescriptionItem saved = captor.getValue();
        assertEquals(30, saved.getQuantity());
        assertEquals("Take after meal", saved.getInstructions());
        assertEquals(matchedMedicine, saved.getMedicine());
    }

    @Test
    void shouldSkipPersistenceWhenPrescriptionMissing() {
        DrugNormalizationService normalizationService = new DrugNormalizationService(defaultLookup(), 0.90, 0.02);
        PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
        PrescriptionItemRepository prescriptionItemRepository = mock(PrescriptionItemRepository.class);
        MedicineRepository medicineRepository = mock(MedicineRepository.class);

        OcrDrugNormalizationService service = new OcrDrugNormalizationService(
                normalizationService,
                prescriptionRepository,
                prescriptionItemRepository,
                medicineRepository
        );

        OcrResultDto ocrResult = new OcrResultDto(true, 0.8, List.of(), null);
        NormalizedOcrResultDto normalizedResult = new NormalizedOcrResultDto(true, 0.8, List.of());

        service.persistPrescriptionOutcome(ocrResult, normalizedResult);

        verify(prescriptionRepository, never()).updateOcrOutcomeById(any(), any(), any());
        verify(prescriptionItemRepository, never()).deleteByPrescriptionId(any());
        verify(prescriptionItemRepository, never()).save(any());
    }

    private DrugLookupService defaultLookup() {
        return new DrugLookupService() {
            @Override
            public List<String> getStandardDrugNames() {
                return List.of("Metformin", "Amoxicillin", "Paracetamol", "Ibuprofen", "Atorvastatin");
            }

            @Override
            public java.util.Map<String, String> getSynonymMappings() {
                return java.util.Map.of(
                        "acetaminophen", "Paracetamol",
                        "tylenol", "Paracetamol"
                );
            }
        };
    }
}
