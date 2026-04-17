package com.TenaMed.antidoping.service.dto;

import com.TenaMed.antidoping.entity.MedicineDopingRuleStatus;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class DopingCheckResult {

    MedicineDopingRuleStatus status;
    List<MatchedSubstance> matchedSubstances;
    String explanationMessage;

    @Value
    @Builder
    public static class MatchedSubstance {
        String ingredientName;
        String category;
        String status;
        String ruleset;
        Integer rulesetYear;
    }
}
