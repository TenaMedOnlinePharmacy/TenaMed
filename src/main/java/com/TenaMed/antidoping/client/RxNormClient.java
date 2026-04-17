package com.TenaMed.antidoping.client;

import com.TenaMed.antidoping.client.dto.RxNormRelatedResponseDto;
import com.TenaMed.antidoping.client.dto.RxNormRxcuiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class RxNormClient {

    private final WebClient webClient;
    private final int timeoutSeconds;

    public RxNormClient(
            WebClient.Builder webClientBuilder,
            @Value("${rxnorm.base-url:https://rxnav.nlm.nih.gov}") String baseUrl,
            @Value("${rxnorm.timeout-seconds:10}") int timeoutSeconds
    ) {
        this.webClient = webClientBuilder.baseUrl(trimTrailingSlash(baseUrl)).build();
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getRxCui(String medicineName) {
        if (medicineName == null || medicineName.isBlank()) {
            return null;
        }

        try {
            RxNormRxcuiResponseDto response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/REST/rxcui.json")
                            .queryParam("name", medicineName.trim())
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.createException().map(ex -> {
                                log.warn("RxNorm getRxCui failed: status={} medicineName={}",
                                        clientResponse.statusCode().value(), medicineName);
                                return ex;
                            }))
                    .bodyToMono(RxNormRxcuiResponseDto.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            if (response == null || response.getIdGroup() == null || response.getIdGroup().getRxnormId() == null) {
                return null;
            }

            return response.getIdGroup().getRxnormId()
                    .stream()
                    .filter(id -> id != null && !id.isBlank())
                    .findFirst()
                    .orElse(null);
        } catch (Exception ex) {
            log.warn("RxNorm getRxCui request failed for medicineName={}: {}", medicineName, ex.getMessage());
            return null;
        }
    }

    public List<String> getIngredients(String rxcui) {
        if (rxcui == null || rxcui.isBlank()) {
            return List.of();
        }

        try {
            RxNormRelatedResponseDto response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/REST/rxcui/{rxcui}/related.json")
                            .queryParam("tty", "IN")
                            .build(rxcui.trim()))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.createException().map(ex -> {
                                log.warn("RxNorm getIngredients failed: status={} rxcui={}",
                                        clientResponse.statusCode().value(), rxcui);
                                return ex;
                            }))
                    .bodyToMono(RxNormRelatedResponseDto.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            return extractIngredientNames(response);
        } catch (Exception ex) {
            log.warn("RxNorm getIngredients request failed for rxcui={}: {}", rxcui, ex.getMessage());
            return List.of();
        }
    }

    private List<String> extractIngredientNames(RxNormRelatedResponseDto response) {
        if (response == null
                || response.getRelatedGroup() == null
                || response.getRelatedGroup().getConceptGroup() == null
                || response.getRelatedGroup().getConceptGroup().isEmpty()) {
            return List.of();
        }

        Set<String> uniqueIngredients = new LinkedHashSet<>();

        for (RxNormRelatedResponseDto.ConceptGroup group : response.getRelatedGroup().getConceptGroup()) {
            if (group == null || group.getConceptProperties() == null) {
                continue;
            }

            for (RxNormRelatedResponseDto.ConceptProperty property : group.getConceptProperties()) {
                if (property == null || property.getName() == null || property.getName().isBlank()) {
                    continue;
                }
                uniqueIngredients.add(property.getName().trim());
            }
        }

        if (uniqueIngredients.isEmpty()) {
            return List.of();
        }

        return new ArrayList<>(uniqueIngredients);
    }

    private String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "https://rxnav.nlm.nih.gov";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
