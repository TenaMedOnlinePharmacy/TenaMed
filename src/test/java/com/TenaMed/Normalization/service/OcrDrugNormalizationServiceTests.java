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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OcrDrugNormalizationServiceTests {

    @Test
    void shouldPreserveOcrEnvelopeAndAppendNormalizationFields() {
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

    Medicine paracetamol = new Medicine();
    paracetamol.setName("Paracetamol");
    when(medicineRepository.findByNameIgnoreCase("Tylenol")).thenReturn(Optional.of(paracetamol));
    when(medicineRepository.findByNameIgnoreCase("zzzzzz")).thenReturn(Optional.empty());
    when(prescriptionRepository.updateOcrOutcomeById(any(), any(), any())).thenReturn(1);

        OcrResultDto ocrResult = new OcrResultDto(
                true,
                0.698,
                List.of(
                        new MedicineOcrItem("Tylenol", null, "tat #30; Sig A.D."),
                        new MedicineOcrItem("zzzzzz", null, "#30; Sig. Once a day")
        ),
        prescription
        );

        NormalizedOcrResultDto result = service.normalize(ocrResult);

        assertTrue(result.isSuccess());
        assertEquals(0.698, result.getConfidence());
        assertEquals(2, result.getMedicines().size());

        assertEquals("Tylenol", result.getMedicines().get(0).getOriginalName());
        assertEquals("Paracetamol", result.getMedicines().get(0).getNormalizedName());
        assertEquals(MatchType.SYNONYM, result.getMedicines().get(0).getMatchType());
        assertEquals(1.0, result.getMedicines().get(0).getConfidence());
        assertEquals(0.698, result.getMedicines().get(0).getOcrConfidence());
        assertFalse(result.getMedicines().get(0).isNeedsReview());
        assertEquals("tat #30; Sig A.D.", result.getMedicines().get(0).getInstruction());

        assertEquals("zzzzzz", result.getMedicines().get(1).getOriginalName());
        assertEquals(null, result.getMedicines().get(1).getNormalizedName());
        assertEquals(MatchType.UNKNOWN, result.getMedicines().get(1).getMatchType());
        assertEquals(0.0, result.getMedicines().get(1).getConfidence());
        assertEquals(0.698, result.getMedicines().get(1).getOcrConfidence());
        assertTrue(result.getMedicines().get(1).isNeedsReview());
        assertEquals("#30; Sig. Once a day", result.getMedicines().get(1).getInstruction());

        verify(prescriptionRepository).updateOcrOutcomeById(eq(prescriptionId), eq(true), eq(0.599));
        verify(prescriptionItemRepository).deleteByPrescriptionId(prescriptionId);
        verify(prescriptionItemRepository, times(1)).save(any());
    }

    @Test
    void shouldHandleNullInputSafely() {
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

        NormalizedOcrResultDto result = service.normalize(null);

        assertFalse(result.isSuccess());
        assertEquals(0.0, result.getConfidence());
        assertTrue(result.getMedicines().isEmpty());

        verify(prescriptionRepository, never()).updateOcrOutcomeById(any(), any(), any());
        verify(prescriptionItemRepository, never()).deleteByPrescriptionId(any());
        verify(prescriptionItemRepository, never()).save(any());
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

        OcrResultDto ocrResult = new OcrResultDto(
            true,
            0.80,
            List.of(new MedicineOcrItem("Tylenol", 30, "Once a day")),
            null
        );

        service.normalize(ocrResult);

        verify(prescriptionRepository, never()).updateOcrOutcomeById(any(), any(), any());
        verify(prescriptionItemRepository, never()).deleteByPrescriptionId(any());
        verify(prescriptionItemRepository, never()).save(any());
        }

        @Test
        void shouldPersistItemWithQuantityAndInstruction() {
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
        when(medicineRepository.findByNameIgnoreCase("Tylenol")).thenReturn(Optional.of(matchedMedicine));
        when(prescriptionRepository.updateOcrOutcomeById(any(), any(), any())).thenReturn(1);

        OcrResultDto ocrResult = new OcrResultDto(
            true,
            0.50,
            List.of(new MedicineOcrItem("Tylenol", 30, "Take after meal")),
            prescription
        );

        service.normalize(ocrResult);

        ArgumentCaptor<com.TenaMed.Normalization.entity.PrescriptionItem> captor =
            ArgumentCaptor.forClass(com.TenaMed.Normalization.entity.PrescriptionItem.class);
        verify(prescriptionItemRepository).save(captor.capture());

        com.TenaMed.Normalization.entity.PrescriptionItem saved = captor.getValue();
        assertEquals(prescription, saved.getPrescription());
        assertEquals(matchedMedicine, saved.getMedicine());
        assertEquals(30, saved.getQuantity());
        assertEquals("Take after meal", saved.getInstructions());
    }

    private DrugLookupService defaultLookup() {
        return new DrugLookupService() {
            @Override
            public List<String> getStandardDrugNames() {
                return List.of(
                        "Metformin",
                        "Amoxicillin",
                        "Paracetamol",
                        "Ibuprofen",
                        "Atorvastatin",
                        "Omeprazole",
                        "Amlodipine",
                        "Losartan",
                        "ascorbic acid",
                        "FeSo43"
                );
            }

            @Override
            public java.util.Map<String, String> getSynonymMappings() {
                return java.util.Map.ofEntries(
                        java.util.Map.entry("acetaminophen", "Paracetamol"),
                        java.util.Map.entry("tylenol", "Paracetamol"),
                        java.util.Map.entry("glucophage", "Metformin"),
                        java.util.Map.entry("amox", "Amoxicillin"),
                        java.util.Map.entry("lipitor", "Atorvastatin"),
                        java.util.Map.entry("norvasc", "Amlodipine"),
                        java.util.Map.entry("cozaar", "Losartan"),
                        java.util.Map.entry("test", "Ascorbic Acid")
                );
            }
        };
    }
}
