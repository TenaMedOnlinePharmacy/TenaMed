package com.TenaMed.antidoping.service;

import com.TenaMed.antidoping.entity.BannedSubstance;
import com.TenaMed.antidoping.entity.BannedSubstanceStatus;
import com.TenaMed.antidoping.entity.MedicineDopingRuleStatus;
import com.TenaMed.antidoping.repository.BannedSubstanceRepository;
import com.TenaMed.antidoping.service.dto.DopingCheckResult;
import com.TenaMed.events.DomainEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DopingCheckService {

    private final IngredientResolverService ingredientResolverService;
    private final BannedSubstanceRepository bannedSubstanceRepository;
    private final DomainEventService domainEventService;

    public DopingCheckService(IngredientResolverService ingredientResolverService,
                              BannedSubstanceRepository bannedSubstanceRepository,
                              DomainEventService domainEventService) {
        this.ingredientResolverService = ingredientResolverService;
        this.bannedSubstanceRepository = bannedSubstanceRepository;
        this.domainEventService = domainEventService;
    }

    @Transactional
    public DopingCheckResult checkMedicine(String medicineName) {
        List<String> ingredients = ingredientResolverService.resolveIngredients(medicineName);
        if (ingredients.isEmpty()) {
            return DopingCheckResult.builder()
                    .status(MedicineDopingRuleStatus.UNKNOWN)
                    .matchedSubstances(List.of())
                    .explanationMessage("No ingredients found for medicine: " + medicineName)
                    .build();
        }

        List<BannedSubstance> matched = bannedSubstanceRepository.findByIngredientNameInIgnoreCase(ingredients);
        if (matched.isEmpty()) {
            return DopingCheckResult.builder()
                    .status(MedicineDopingRuleStatus.SAFE)
                    .matchedSubstances(List.of())
                    .explanationMessage("No banned or restricted substances found")
                    .build();
        }

        MedicineDopingRuleStatus finalStatus = resolveStatus(matched);
        List<DopingCheckResult.MatchedSubstance> matchedSubstances = toMatchedSubstances(matched);
        String message = buildMessage(finalStatus, matchedSubstances);

        DopingCheckResult result = DopingCheckResult.builder()
                .status(finalStatus)
                .matchedSubstances(matchedSubstances)
                .explanationMessage(message)
                .build();

        domainEventService.publish(
            "DOPING_CHECK_EXECUTED",
            "DOPING_CHECK",
            null,
            "PLATFORM",
            null,
            Map.of("medicineName", medicineName, "status", finalStatus.name(), "matches", matchedSubstances.size())
        );

        return result;
    }

    private MedicineDopingRuleStatus resolveStatus(List<BannedSubstance> matchedSubstances) {
        boolean hasBanned = matchedSubstances.stream()
                .anyMatch(item -> item.getStatus() == BannedSubstanceStatus.BANNED);
        if (hasBanned) {
            return MedicineDopingRuleStatus.BANNED;
        }

        boolean hasRestricted = matchedSubstances.stream()
                .anyMatch(item -> item.getStatus() == BannedSubstanceStatus.RESTRICTED);
        if (hasRestricted) {
            return MedicineDopingRuleStatus.RESTRICTED;
        }

        return MedicineDopingRuleStatus.SAFE;
    }

    private List<DopingCheckResult.MatchedSubstance> toMatchedSubstances(List<BannedSubstance> matchedSubstances) {
        Set<String> seen = matchedSubstances.stream()
                .map(this::buildDedupeKey)
                .collect(Collectors.toSet());

        List<DopingCheckResult.MatchedSubstance> result = new ArrayList<>();
        for (BannedSubstance substance : matchedSubstances) {
            String dedupeKey = buildDedupeKey(substance);
            if (!seen.remove(dedupeKey)) {
                continue;
            }
            result.add(DopingCheckResult.MatchedSubstance.builder()
                    .ingredientName(substance.getIngredientName())
                    .category(substance.getCategory())
                    .status(substance.getStatus() == null ? null : substance.getStatus().name())
                    .ruleset(substance.getRuleset())
                    .rulesetYear(substance.getRulesetYear())
                    .build());
        }
        return result;
    }

    private String buildMessage(MedicineDopingRuleStatus status, List<DopingCheckResult.MatchedSubstance> matched) {
        if (status == MedicineDopingRuleStatus.BANNED) {
            return "Banned substance detected (" + matched.size() + " match(es))";
        }
        if (status == MedicineDopingRuleStatus.RESTRICTED) {
            return "Restricted substance detected (" + matched.size() + " match(es))";
        }
        return "No banned or restricted substances found";
    }

    private String buildDedupeKey(BannedSubstance substance) {
        String ingredient = substance.getIngredientName() == null
                ? ""
                : substance.getIngredientName().trim().toLowerCase(Locale.ROOT);
        String status = substance.getStatus() == null ? "" : substance.getStatus().name();
        String ruleset = substance.getRuleset() == null ? "" : substance.getRuleset().trim().toLowerCase(Locale.ROOT);
        String year = substance.getRulesetYear() == null ? "" : substance.getRulesetYear().toString();
        return ingredient + "|" + status + "|" + ruleset + "|" + year;
    }
}
