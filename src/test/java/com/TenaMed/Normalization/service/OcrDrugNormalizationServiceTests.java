package com.TenaMed.Normalization.service;

import com.TenaMed.Normalization.model.MatchType;
import com.TenaMed.Normalization.model.NormalizedOcrResultDto;
import com.TenaMed.ocr.dto.MedicineOcrItem;
import com.TenaMed.ocr.dto.OcrResultDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcrDrugNormalizationServiceTests {

    @Test
    void shouldPreserveOcrEnvelopeAndAppendNormalizationFields() {
        DrugNormalizationService normalizationService = new DrugNormalizationService(new InMemoryDrugLookupService(), 0.90, 0.02);
        OcrDrugNormalizationService service = new OcrDrugNormalizationService(normalizationService);

        OcrResultDto ocrResult = new OcrResultDto(
                true,
                0.698,
                List.of(
                        new MedicineOcrItem("Tylenol", null, "tat #30; Sig A.D."),
                        new MedicineOcrItem("zzzzzz", null, "#30; Sig. Once a day")
                )
        );

        NormalizedOcrResultDto result = service.normalize(ocrResult);

        assertTrue(result.isSuccess());
        assertEquals(0.698, result.getConfidence());
        assertEquals(2, result.getMedicines().size());

        assertEquals("Tylenol", result.getMedicines().get(0).getOriginalName());
        assertEquals("Paracetamol", result.getMedicines().get(0).getNormalizedName());
        assertEquals(MatchType.SYNONYM, result.getMedicines().get(0).getMatchType());
        assertEquals(1.0, result.getMedicines().get(0).getConfidence());
        assertFalse(result.getMedicines().get(0).isNeedsReview());
        assertEquals("tat #30; Sig A.D.", result.getMedicines().get(0).getInstruction());

        assertEquals("zzzzzz", result.getMedicines().get(1).getOriginalName());
        assertEquals(null, result.getMedicines().get(1).getNormalizedName());
        assertEquals(MatchType.UNKNOWN, result.getMedicines().get(1).getMatchType());
        assertEquals(0.0, result.getMedicines().get(1).getConfidence());
        assertTrue(result.getMedicines().get(1).isNeedsReview());
        assertEquals("#30; Sig. Once a day", result.getMedicines().get(1).getInstruction());
    }

    @Test
    void shouldHandleNullInputSafely() {
        DrugNormalizationService normalizationService = new DrugNormalizationService(new InMemoryDrugLookupService(), 0.90, 0.02);
        OcrDrugNormalizationService service = new OcrDrugNormalizationService(normalizationService);

        NormalizedOcrResultDto result = service.normalize(null);

        assertFalse(result.isSuccess());
        assertEquals(0.0, result.getConfidence());
        assertTrue(result.getMedicines().isEmpty());
    }
}
