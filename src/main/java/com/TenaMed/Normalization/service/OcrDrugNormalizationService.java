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

        return new NormalizedOcrResultDto(ocrResult.isSuccess(), ocrResult.getConfidence(), merged);
    }

    @Transactional
    public void persistPrescriptionOutcome(
            OcrResultDto ocrResult,
            NormalizedOcrResultDto normalizedResult
    ) {
        if (ocrResult == null || normalizedResult == null) {
            return;
        }

        Prescription prescription = ocrResult.getPrescription();
        if (prescription == null || prescription.getId() == null) {
            return;
        }

        List<NormalizedOcrMedicineItem> normalizedMedicines = normalizedResult.getMedicines() == null
                ? List.of()
                : normalizedResult.getMedicines();

        UUID prescriptionId = prescription.getId();
        double avgNormalizedConfidence = averageNormalizedConfidence(normalizedMedicines);
        double prescriptionConfidenceScore = (avgNormalizedConfidence + ocrResult.getConfidence()) / 2.0;

        prescriptionRepository.updateOcrOutcomeById(
                prescriptionId,
                ocrResult.isSuccess(),
                prescriptionConfidenceScore
        );

        prescriptionItemRepository.deleteByPrescriptionId(prescriptionId);
        for (NormalizedOcrMedicineItem normalizedItem : normalizedMedicines) {
            String originalName = normalizedItem == null ? null : normalizedItem.getOriginalName();
            String normalizedName = normalizedItem == null ? null : normalizedItem.getNormalizedName();
            if ((originalName == null || originalName.isBlank())
                    && (normalizedName == null || normalizedName.isBlank())) {
                continue;
            }

            Medicine medicine = findMedicineByName(normalizedName);
            if (medicine == null) {
                medicine = findMedicineByName(originalName);
            }
            if (medicine == null) {
                continue;
            }

            PrescriptionItem item = new PrescriptionItem();
            item.setPrescription(prescription);
            item.setMedicine(medicine);
            item.setQuantity(normalizedItem == null ? null : normalizedItem.getQuantity());
            item.setInstructions(normalizedItem == null ? null : normalizedItem.getInstruction());
            prescriptionItemRepository.save(item);
        }
    }

    private Medicine findMedicineByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        var found = medicineRepository.findByNameIgnoreCase(name);
        return found == null ? null : found.orElse(null);
    }

    private double averageNormalizedConfidence(List<NormalizedOcrMedicineItem> normalized) {
        if (normalized == null || normalized.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        int count = 0;
        for (NormalizedOcrMedicineItem item : normalized) {
            if (item == null) {
                continue;
            }
            total += item.getConfidence();
            count++;
        }

        return count == 0 ? 0.0 : total / count;
    }
}
