package com.TenaMed.ocr.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.TenaMed.ocr.dto.MedicineOcrItem;
import com.TenaMed.ocr.dto.OcrResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import com.TenaMed.ocr.integration.OcrClient;


@Slf4j
@Component
public class VeryfiOcrClient implements OcrClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String processUrl;
    private final int timeoutSeconds;

    public VeryfiOcrClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${ocr.veryfi.process-url:https://api.veryfi.com/api/v8/partner/any-documents}") String processUrl,
            @Value("${ocr.veryfi.client-id:}") String clientId,
            @Value("${ocr.veryfi.api-key:}") String apiKey,
            @Value("${ocr.veryfi.username:}") String username,
            @Value("${ocr.veryfi.timeout-seconds:15}") int timeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.processUrl = processUrl;
        this.timeoutSeconds = timeoutSeconds;

        this.webClient = webClientBuilder
                .defaultHeader("CLIENT-ID", clientId)
                .defaultHeader("AUTHORIZATION", "apikey " + username + ":" + apiKey)
                .build();
    }

    @Override
    public OcrResultDto processPrescription(String imageUrl) {
        log.info("Sending OCR request to Veryfi endpoint={} imageUrlHost={}", processUrl, safeHost(imageUrl));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("file_url", imageUrl);

        try {
            String responseBody = webClient.post()
                    .uri(processUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .bodyValue(formData)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(errorBody -> {
                                log.error("Veryfi OCR 4xx error: status={} body={}", response.statusCode().value(), summarize(errorBody));
                                return new IllegalStateException("Veryfi OCR client error");
                            }))
                    .onStatus(HttpStatusCode::is5xxServerError, response -> response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(errorBody -> {
                                log.error("Veryfi OCR 5xx error: status={} body={}", response.statusCode().value(), summarize(errorBody));
                                return new IllegalStateException("Veryfi OCR server error");
                            }))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            if (responseBody == null || responseBody.isBlank()) {
                log.error("Veryfi OCR returned empty response body");
                return failureResult();
            }

            OcrResultDto result = mapResponse(responseBody);
            log.info(
                    "OCR response received: success={} confidence={} medicinesCount={}",
                    result.isSuccess(),
                    result.getConfidence(),
                    result.getMedicines() == null ? 0 : result.getMedicines().size()
            );
            return result;
        } catch (WebClientResponseException ex) {
            log.error("Veryfi OCR API error: status={} endpoint={}", ex.getStatusCode().value(), processUrl);
            return failureResult();
        } catch (WebClientRequestException ex) {
            boolean timedOut = ex.getCause() instanceof TimeoutException;
            if (timedOut) {
                log.error("Veryfi OCR request timed out after {} seconds", timeoutSeconds);
            } else {
                log.error("Veryfi OCR request failed: {}", ex.getMessage());
            }
            return failureResult();
        } catch (Exception ex) {
            log.error("Failed to process Veryfi OCR response", ex);
            return failureResult();
        }
    }

    private OcrResultDto mapResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        double confidence = root.path("confidence").asDouble(0.0);
        List<MedicineOcrItem> medicines = extractMedicines(root);

        return new OcrResultDto(true, confidence, medicines);
    }

    private List<MedicineOcrItem> extractMedicines(JsonNode root) {
        List<MedicineOcrItem> items = new ArrayList<>();
        JsonNode medicinesNode = root.path("medicines");

        if (!medicinesNode.isArray()) {
            medicinesNode = root.path("line_items");
        }

        if (!medicinesNode.isArray()) {
            return items;
        }

        for (JsonNode node : medicinesNode) {
            MedicineOcrItem item = new MedicineOcrItem(
                    asText(node, "name", "description", "drug_name"),
                    Integer.getInteger(asText(node, "quantity", "dose")),
                    asText(node, "frequency", "schedule")
            );
            items.add(item);
        }

        return items;
    }

    private String asText(JsonNode node, String... candidateKeys) {
        for (String key : candidateKeys) {
            JsonNode valueNode = node.path(key);
            if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                String value = valueNode.asText("").trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return "";
    }

    private String safeHost(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return "unknown";
        }
        try {
            return java.net.URI.create(imageUrl).getHost();
        } catch (Exception ignored) {
            return "invalid-url";
        }
    }

    private OcrResultDto failureResult() {
        return new OcrResultDto(false, 0.0, List.of());
    }

    private String summarize(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String trimmed = content.replaceAll("\\s+", " ").trim();
        int maxLength = 300;
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength) + "...";
    }
}
