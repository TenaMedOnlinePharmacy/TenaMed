package com.TenaMed.Normalization.service;

import com.TenaMed.Normalization.entity.PrescriptionItem;
import com.TenaMed.Normalization.model.InputMedicine;
import com.TenaMed.Normalization.model.MatchType;
import com.TenaMed.Normalization.model.NormalizedMedicine;
import com.TenaMed.Normalization.model.NormalizedOcrMedicineItem;
import com.TenaMed.Normalization.model.NormalizedOcrResultDto;
import com.TenaMed.Normalization.repository.PrescriptionItemRepository;
import com.TenaMed.medicine.entity.Medicine;
import com.TenaMed.medicine.repository.MedicineRepository;
import com.TenaMed.ocr.dto.MedicineOcrItem;
import com.TenaMed.ocr.dto.OcrResultDto;
import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OcrDrugNormalizationService {

    private final DrugNormalizationService drugNormalizationService;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final MedicineRepository medicineRepository;

    public OcrDrugNormalizationService(
            DrugNormalizationService drugNormalizationService,
            PrescriptionRepository prescriptionRepository,
            PrescriptionItemRepository prescriptionItemRepository,
            MedicineRepository medicineRepository
    ) {
        this.drugNormalizationService = drugNormalizationService;
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.medicineRepository = medicineRepository;
    }

    @Transactional
    public NormalizedOcrResultDto normalize(OcrResultDto ocrResult) {
        if (ocrResult == null) {
            return new NormalizedOcrResultDto(false, 0.0, List.of());
        }

        List<MedicineOcrItem> ocrMedicines = ocrResult.getMedicines() == null ? List.of() : ocrResult.getMedicines();

        List<InputMedicine> inputs = ocrMedicines.stream()
                .map(item -> new InputMedicine(item == null ? null : item.getName()))
                .toList();

        List<NormalizedMedicine> normalized = drugNormalizationService.normalizeAll(inputs);

        List<NormalizedOcrMedicineItem> merged = new ArrayList<>();
        for (int i = 0; i < ocrMedicines.size(); i++) {
            MedicineOcrItem ocrItem = ocrMedicines.get(i);
            NormalizedMedicine normalizedItem = i < normalized.size() ? normalized.get(i) : null;

            String originalName = ocrItem == null ? null : ocrItem.getName();
            String normalizedName = normalizedItem == null ? null : normalizedItem.getNormalizedName();
            MatchType matchType = normalizedItem == null ? MatchType.UNKNOWN : normalizedItem.getMatchType();
                double confidence = normalizedItem == null ? 0.0 : normalizedItem.getConfidence();
                double ocrConfidence = ocrResult.getConfidence();
            boolean needsReview = normalizedItem == null
                    || normalizedItem.isNeedsReview()
                    || matchType == MatchType.UNKNOWN;
            Integer quantity = ocrItem == null ? null : ocrItem.getQuantity();
            String instruction = ocrItem == null ? null : ocrItem.getInstruction();

                if (normalizedItem != null) {
                normalizedItem.setOcrConfidence(ocrConfidence);
                }

            merged.add(new NormalizedOcrMedicineItem(
                    originalName,
                    normalizedName,
                    matchType,
                    confidence,
                    ocrConfidence,
                    needsReview,
                    quantity,
                    instruction
            ));
        }

        persistPrescriptionOutcome(ocrResult, ocrMedicines, normalized);

        return new NormalizedOcrResultDto(ocrResult.isSuccess(), ocrResult.getConfidence(), merged);
    }

    private void persistPrescriptionOutcome(
            OcrResultDto ocrResult,
            List<MedicineOcrItem> ocrMedicines,
            List<NormalizedMedicine> normalized
    ) {
        Prescription prescription = ocrResult.getPrescription();
        if (prescription == null || prescription.getId() == null) {
            return;
        }

        UUID prescriptionId = prescription.getId();
        double avgNormalizedConfidence = averageNormalizedConfidence(normalized);
        double prescriptionConfidenceScore = (avgNormalizedConfidence + ocrResult.getConfidence()) / 2.0;

        prescriptionRepository.updateOcrOutcomeById(
                prescriptionId,
                ocrResult.isSuccess(),
                prescriptionConfidenceScore
        );

        prescriptionItemRepository.deleteByPrescriptionId(prescriptionId);
        for (MedicineOcrItem ocrItem : ocrMedicines) {
            if (ocrItem == null || ocrItem.getName() == null || ocrItem.getName().isBlank()) {
                continue;
            }

            Medicine medicine = medicineRepository.findByNameIgnoreCase(ocrItem.getName()).orElse(null);
            if (medicine == null) {
                continue;
            }

            PrescriptionItem item = new PrescriptionItem();
            item.setPrescription(prescription);
            item.setMedicine(medicine);
            item.setQuantity(ocrItem.getQuantity());
            item.setInstructions(ocrItem.getInstruction());
            prescriptionItemRepository.save(item);
        }
    }

    private double averageNormalizedConfidence(List<NormalizedMedicine> normalized) {
        if (normalized == null || normalized.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        int count = 0;
        for (NormalizedMedicine item : normalized) {
            if (item == null) {
                continue;
            }
            total += item.getConfidence();
            count++;
        }

        return count == 0 ? 0.0 : total / count;
    }
}
