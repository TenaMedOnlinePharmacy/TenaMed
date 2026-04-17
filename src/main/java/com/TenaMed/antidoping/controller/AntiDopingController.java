package com.TenaMed.antidoping.controller;

import com.TenaMed.antidoping.dto.request.DopingCheckRequest;
import com.TenaMed.antidoping.dto.response.DopingCheckResponse;
import com.TenaMed.antidoping.service.AthleteValidationService;
import com.TenaMed.antidoping.service.DopingCheckService;
import com.TenaMed.antidoping.service.dto.DopingCheckResult;
import com.TenaMed.common.exception.UnauthorizedException;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/anti-doping")
public class AntiDopingController {

    private final AthleteValidationService athleteValidationService;
    private final DopingCheckService dopingCheckService;

    public AntiDopingController(AthleteValidationService athleteValidationService,
                                DopingCheckService dopingCheckService) {
        this.athleteValidationService = athleteValidationService;
        this.dopingCheckService = dopingCheckService;
    }

    @PostMapping("/check")
    public ResponseEntity<DopingCheckResponse> check(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody DopingCheckRequest request
    ) {
        UUID userId = extractUserId(principal);
        athleteValidationService.validateAthlete(userId);

        DopingCheckResult result = dopingCheckService.checkMedicine(request.getMedicineName());
        return ResponseEntity.ok(toResponse(result));
    }

    private UUID extractUserId(AuthenticatedUserPrincipal principal) {
        if (principal == null || principal.getUserId() == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal.getUserId();
    }

    private DopingCheckResponse toResponse(DopingCheckResult result) {
        List<DopingCheckResponse.MatchedSubstanceResponse> matches = result.getMatchedSubstances().stream()
                .map(item -> DopingCheckResponse.MatchedSubstanceResponse.builder()
                        .ingredientName(item.getIngredientName())
                        .category(item.getCategory())
                        .status(item.getStatus())
                        .ruleset(item.getRuleset())
                        .rulesetYear(item.getRulesetYear())
                        .build())
                .toList();

        return DopingCheckResponse.builder()
                .status(result.getStatus().name())
                .matchedSubstances(matches)
                .message(result.getExplanationMessage())
                .build();
    }
}
