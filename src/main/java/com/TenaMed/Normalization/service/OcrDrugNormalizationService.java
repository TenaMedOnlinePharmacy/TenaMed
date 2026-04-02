package com.TenaMed.Normalization.service;

import com.TenaMed.Normalization.model.InputMedicine;
import com.TenaMed.Normalization.model.MatchType;
import com.TenaMed.Normalization.model.NormalizedMedicine;
import com.TenaMed.Normalization.model.NormalizedOcrMedicineItem;
import com.TenaMed.Normalization.model.NormalizedOcrResultDto;
import com.TenaMed.ocr.dto.MedicineOcrItem;
import com.TenaMed.ocr.dto.OcrResultDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OcrDrugNormalizationService {

    private final DrugNormalizationService drugNormalizationService;

    public OcrDrugNormalizationService(DrugNormalizationService drugNormalizationService) {
        this.drugNormalizationService = drugNormalizationService;
    }

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
            boolean needsReview = normalizedItem == null
                    || normalizedItem.isNeedsReview()
                    || matchType == MatchType.UNKNOWN;
            Integer quantity = ocrItem == null ? null : ocrItem.getQuantity();
            String instruction = ocrItem == null ? null : ocrItem.getInstruction();

            merged.add(new NormalizedOcrMedicineItem(
                    originalName,
                    normalizedName,
                    matchType,
                    confidence,
                    needsReview,
                    quantity,
                    instruction
            ));
        }

        return new NormalizedOcrResultDto(ocrResult.isSuccess(), ocrResult.getConfidence(), merged);
    }
}
