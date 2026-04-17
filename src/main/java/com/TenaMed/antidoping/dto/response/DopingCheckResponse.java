package com.TenaMed.antidoping.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DopingCheckResponse {

    private String status;
    private List<MatchedSubstanceResponse> matchedSubstances;
    private String message;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MatchedSubstanceResponse {
        private String ingredientName;
        private String category;
        private String status;
        private String ruleset;
        private Integer rulesetYear;
    }
}
