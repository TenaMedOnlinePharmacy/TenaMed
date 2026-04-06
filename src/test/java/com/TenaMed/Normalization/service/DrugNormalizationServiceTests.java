package com.TenaMed.Normalization.service;

import com.TenaMed.Normalization.model.InputMedicine;
import com.TenaMed.Normalization.model.MatchType;
import com.TenaMed.Normalization.model.NormalizedMedicine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrugNormalizationServiceTests {

    @Test
    void shouldNormalizeByExactMatch() {
        DrugNormalizationService service = new DrugNormalizationService(defaultLookup(), 0.90, 0.02);

        NormalizedMedicine result = service.normalize(new InputMedicine("Metformin"));

        assertEquals(MatchType.EXACT, result.getMatchType());
        assertEquals("Metformin", result.getNormalizedName());
        assertEquals("Metformin", result.getOriginalName());
        assertEquals(1.0, result.getConfidence());
        assertTrue(!result.isNeedsReview());
    }

    @Test
    void shouldNormalizeBySynonym() {
        DrugNormalizationService service = new DrugNormalizationService(defaultLookup(), 0.90, 0.02);

        NormalizedMedicine result = service.normalize(new InputMedicine("Tylenol"));

        assertEquals(MatchType.SYNONYM, result.getMatchType());
        assertEquals("Paracetamol", result.getNormalizedName());
        assertEquals("Tylenol", result.getOriginalName());
        assertEquals(1.0, result.getConfidence());
        assertTrue(!result.isNeedsReview());
    }

    @Test
    void shouldNormalizeByFuzzyWhenSafe() {
        DrugNormalizationService service = new DrugNormalizationService(defaultLookup(), 0.90, 0.02);

        NormalizedMedicine result = service.normalize(new InputMedicine("Metfornin"));

        assertEquals(MatchType.FUZZY, result.getMatchType());
        assertEquals("Metformin", result.getNormalizedName());
        assertTrue(result.getConfidence() >= 0.90);
        assertTrue(!result.isNeedsReview());
    }

    @Test
    void shouldReturnUnknownWhenAmbiguousFuzzyMatch() {
        DrugLookupService ambiguousLookup = new DrugLookupService() {
            @Override
            public List<String> getStandardDrugNames() {
                return List.of("Drugax", "Drugay");
            }

            @Override
            public Map<String, String> getSynonymMappings() {
                return Map.of();
            }
        };

        DrugNormalizationService service = new DrugNormalizationService(ambiguousLookup, 0.80, 0.05);

        NormalizedMedicine result = service.normalize(new InputMedicine("Druga"));

        assertEquals(MatchType.UNKNOWN, result.getMatchType());
        assertEquals("Druga", result.getOriginalName());
        assertEquals(null, result.getNormalizedName());
        assertTrue(result.isNeedsReview());
    }

    @Test
    void shouldReturnUnknownWhenNoReliableMatch() {
        DrugNormalizationService service = new DrugNormalizationService(defaultLookup(), 0.90, 0.02);

        NormalizedMedicine result = service.normalize(new InputMedicine("zzzzzz"));

        assertEquals(MatchType.UNKNOWN, result.getMatchType());
        assertEquals("zzzzzz", result.getOriginalName());
        assertEquals(null, result.getNormalizedName());
        assertEquals(0.0, result.getConfidence());
        assertTrue(result.isNeedsReview());
    }

    @Test
    void shouldReturnUnknownForShortInputAndSkipFuzzy() {
        DrugNormalizationService service = new DrugNormalizationService(defaultLookup(), 0.90, 0.02);

        NormalizedMedicine result = service.normalize(new InputMedicine("am"));

        assertEquals(MatchType.UNKNOWN, result.getMatchType());
        assertEquals("am", result.getOriginalName());
        assertEquals(null, result.getNormalizedName());
        assertEquals(0.0, result.getConfidence());
        assertTrue(result.isNeedsReview());
    }

    @Test
    void shouldIgnoreSynonymThatPointsToInvalidStandardDrug() {
        DrugLookupService invalidSynonymLookup = new DrugLookupService() {
            @Override
            public List<String> getStandardDrugNames() {
                return List.of("Metformin");
            }

            @Override
            public Map<String, String> getSynonymMappings() {
                return Map.of("met", "Metfornin");
            }
        };

        DrugNormalizationService service = new DrugNormalizationService(invalidSynonymLookup, 0.90, 0.02);

        NormalizedMedicine result = service.normalize(new InputMedicine("met"));

        assertEquals(MatchType.UNKNOWN, result.getMatchType());
        assertEquals(0.0, result.getConfidence());
        assertTrue(result.isNeedsReview());
    }

    @Test
    void shouldEnforceMinimumFuzzyThresholdOfNinetyPercent() {
        DrugLookupService lookup = new DrugLookupService() {
            @Override
            public List<String> getStandardDrugNames() {
                return List.of("Metformin");
            }

            @Override
            public Map<String, String> getSynonymMappings() {
                return Map.of();
            }
        };

        DrugNormalizationService service = new DrugNormalizationService(lookup, 0.50, 0.02);
        NormalizedMedicine result = service.normalize(new InputMedicine("metx"));

        assertEquals(MatchType.UNKNOWN, result.getMatchType());
        assertEquals(0.0, result.getConfidence());
        assertTrue(result.isNeedsReview());
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
            public Map<String, String> getSynonymMappings() {
                return Map.ofEntries(
                        Map.entry("acetaminophen", "Paracetamol"),
                        Map.entry("tylenol", "Paracetamol"),
                        Map.entry("glucophage", "Metformin"),
                        Map.entry("amox", "Amoxicillin"),
                        Map.entry("lipitor", "Atorvastatin"),
                        Map.entry("norvasc", "Amlodipine"),
                        Map.entry("cozaar", "Losartan"),
                        Map.entry("test", "Ascorbic Acid")
                );
            }
        };
    }
}
