package com.TenaMed.Normalization.service;

import com.TenaMed.Normalization.model.InputMedicine;
import com.TenaMed.Normalization.model.MatchType;
import com.TenaMed.Normalization.model.NormalizedMedicine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class DrugNormalizationService {

    private static final int MIN_FUZZY_INPUT_LENGTH = 3;
    private static final int MAX_INPUT_LOG_LENGTH = 64;

    private final double fuzzyAcceptanceThreshold;
    private final double ambiguityDelta;
    private final Map<String, String> standardByNormalized;
    private final Map<String, String> synonymsByNormalized;
    private final Map<String, String> fuzzyCandidatesByNormalized;
    private final JaroWinklerSimilarity similarity;

    public DrugNormalizationService(
            DrugLookupService drugLookupService,
            @Value("${normalization.fuzzy.acceptance-threshold}") double fuzzyAcceptanceThreshold,
            @Value("${normalization.fuzzy.ambiguity-delta:0.02}") double ambiguityDelta
    ) {
        this.fuzzyAcceptanceThreshold = Math.max(0.70, fuzzyAcceptanceThreshold);
        this.ambiguityDelta = ambiguityDelta;
        this.similarity = new JaroWinklerSimilarity();

        Map<String, String> standardLookup = buildStandardLookup(drugLookupService.getStandardDrugNames());
        this.standardByNormalized = Map.copyOf(standardLookup);

        Map<String, String> synonymLookup = buildNormalizedSynonymLookup(
            drugLookupService.getSynonymMappings(),
            standardLookup
        );
        this.synonymsByNormalized = Map.copyOf(synonymLookup);

        Map<String, String> fuzzyLookup = new LinkedHashMap<>(standardLookup);
        synonymLookup.forEach(fuzzyLookup::putIfAbsent);
        this.fuzzyCandidatesByNormalized = Map.copyOf(fuzzyLookup);
    }

    public NormalizedMedicine normalize(InputMedicine input) {
        String originalName = input == null ? null : input.getName();
        String normalizedInput = normalizeText(originalName);

        if (normalizedInput == null) {
            NormalizedMedicine decision = unknown(originalName);
            logDecision(originalName, MatchType.UNKNOWN, null, decision.getConfidence(), "EMPTY_INPUT");
            return decision;
        }

        String exactMatch = standardByNormalized.get(normalizedInput);
        if (exactMatch != null) {
            NormalizedMedicine decision = new NormalizedMedicine(originalName, exactMatch, MatchType.EXACT, 1.0, false);
            logDecision(originalName, MatchType.EXACT, exactMatch, decision.getConfidence(), "EXACT_MATCH");
            return decision;
        }

        String synonymMatch = synonymsByNormalized.get(normalizedInput);
        if (synonymMatch != null) {
            NormalizedMedicine decision = new NormalizedMedicine(originalName, synonymMatch, MatchType.SYNONYM, 1.0, false);
            logDecision(originalName, MatchType.SYNONYM, synonymMatch, decision.getConfidence(), "SYNONYM_MATCH");
            return decision;
        }

        if (normalizedInput.length() < MIN_FUZZY_INPUT_LENGTH) {
            NormalizedMedicine decision = unknown(originalName);
            logDecision(originalName, MatchType.UNKNOWN, null, decision.getConfidence(), "SHORT_INPUT");
            return decision;
        }

        FuzzyCandidate bestCandidate = findBestFuzzyMatch(normalizedInput, fuzzyCandidatesByNormalized);
        if (!bestCandidate.accepted()) {
            NormalizedMedicine decision = unknown(originalName);
            logDecision(originalName, MatchType.UNKNOWN, null, decision.getConfidence(), "FUZZY_REJECTED");
            return decision;
        }

        NormalizedMedicine decision = new NormalizedMedicine(
                originalName,
                bestCandidate.bestDrug(),
                MatchType.FUZZY,
                bestCandidate.bestScore(),
                false
        );
        logDecision(originalName, MatchType.FUZZY, bestCandidate.bestDrug(), decision.getConfidence(), "FUZZY_ACCEPTED");
        return decision;
    }

    public List<NormalizedMedicine> normalizeAll(List<InputMedicine> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }

        List<NormalizedMedicine> result = new ArrayList<>();
        for (InputMedicine input : inputs) {
            result.add(normalize(input));
        }
        return result;
    }

    private NormalizedMedicine unknown(String originalName) {
        return new NormalizedMedicine(originalName, null, MatchType.UNKNOWN, 0.0, true);
    }

    private Map<String, String> buildStandardLookup(List<String> standardDrugNames) {
        Map<String, String> lookup = new HashMap<>();
        Map<String, List<String>> collisions = new LinkedHashMap<>();

        for (String drug : standardDrugNames) {
            String normalized = normalizeText(drug);
            if (normalized != null) {
                String existing = lookup.putIfAbsent(normalized, drug);
                if (existing != null && !existing.equals(drug)) {
                    collisions.computeIfAbsent(normalized, key -> {
                        List<String> drugs = new ArrayList<>();
                        drugs.add(existing);
                        return drugs;
                    }).add(drug);
                }
            }
        }

        if (!collisions.isEmpty()) {
            log.warn("normalization_standard_collision_detected keys={} collisionCount={}", collisions.keySet(), collisions.size());
            if (log.isDebugEnabled()) {
                log.debug("normalization_standard_collision_details details={}", collisions);
            }
        }

        return lookup;
    }

    private Map<String, String> buildNormalizedSynonymLookup(
            Map<String, String> rawSynonyms,
            Map<String, String> standardLookup
    ) {
        Map<String, String> lookup = new LinkedHashMap<>();
        Set<String> validNormalizedStandardTargets = standardLookup.keySet();

        for (Map.Entry<String, String> entry : rawSynonyms.entrySet()) {
            String normalizedKey = normalizeText(entry.getKey());
            String target = entry.getValue();
            String normalizedTarget = normalizeText(target);

            if (normalizedKey == null || normalizedTarget == null) {
                continue;
            }

            if (!validNormalizedStandardTargets.contains(normalizedTarget)) {
                log.warn("normalization_invalid_synonym_target synonym={} target={}", normalizedKey, normalizedTarget);
                continue;
            }

            lookup.putIfAbsent(normalizedKey, standardLookup.get(normalizedTarget));
        }

        return lookup;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.toLowerCase(Locale.ROOT).trim();
        if (normalized.isBlank()) {
            return null;
        }

        normalized = normalized.replaceAll("[^a-z0-9 ]", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized.isBlank() ? null : normalized;
    }

    private FuzzyCandidate findBestFuzzyMatch(String normalizedInput, Map<String, String> standardByNormalized) {
        if (standardByNormalized.isEmpty()) {
            return new FuzzyCandidate(null, 0.0, 0.0, false);
        }

        String bestDrug = null;
        double bestScore = -1.0;
        double secondBestScore = -1.0;

        for (Map.Entry<String, String> entry : standardByNormalized.entrySet()) {
            double score = similarity.apply(normalizedInput, entry.getKey());
            if (score > bestScore) {
                secondBestScore = bestScore;
                bestScore = score;
                bestDrug = entry.getValue();
            } else if (score > secondBestScore) {
                secondBestScore = score;
            }
        }

        if (bestScore < 0.0) {
            return new FuzzyCandidate(null, 0.0, 0.0, false);
        }

        if (secondBestScore < 0.0) {
            secondBestScore = 0.0;
        }

        boolean scoreHighEnough = bestScore >= fuzzyAcceptanceThreshold;
        boolean ambiguous = secondBestScore >= fuzzyAcceptanceThreshold
                && (bestScore - secondBestScore) <= ambiguityDelta;

        boolean accepted = scoreHighEnough && !ambiguous;

        return new FuzzyCandidate(bestDrug, bestScore, secondBestScore, accepted);
    }

    private void logDecision(String rawInput, MatchType matchType, String selectedDrug, double confidence, String reason) {
        log.info(
                "normalization_decision input={} matchType={} selectedDrug={} confidence={} needsReview={} reason={}",
                sanitizeInputForLog(rawInput),
                matchType,
                selectedDrug,
                confidence,
                matchType == MatchType.UNKNOWN,
                reason
        );
    }

    private String sanitizeInputForLog(String value) {
        if (value == null) {
            return "<null>";
        }

        String normalized = normalizeText(value);
        if (normalized == null) {
            return "<blank>";
        }

        if (normalized.length() <= MAX_INPUT_LOG_LENGTH) {
            return normalized;
        }

        return normalized.substring(0, MAX_INPUT_LOG_LENGTH) + "...";
    }

    private record FuzzyCandidate(String bestDrug, double bestScore, double secondBestScore, boolean accepted) {
    }
}
