package com.TenaMed.ocr.integration;

import com.TenaMed.ocr.dto.OcrResultDto;
import com.TenaMed.ocr.dto.external.VeryfiOcrResponseDto;
import com.TenaMed.ocr.mapper.OcrMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import com.TenaMed.ocr.integration.OcrClient;


@Slf4j
@Component
public class VeryfiOcrClient implements OcrClient {

    private final WebClient webClient;
    private final OcrMapper ocrMapper;
    private final String processUrl;
    private final int timeoutSeconds;

    public VeryfiOcrClient(
            WebClient.Builder webClientBuilder,
            @Value("${ocr.veryfi.environment-url:https://api.veryfi.com}") String environmentUrl,
            @Value("${ocr.veryfi.process-endpoint:/api/v8/partner/any-documents}") String processEndpoint,
            @Value("${ocr.veryfi.client-id:}") String clientId,
            @Value("${ocr.veryfi.client-secret:}") String clientSecret,
            @Value("${ocr.veryfi.api-key:}") String apiKey,
            @Value("${ocr.veryfi.username:}") String username,
            @Value("${ocr.veryfi.timeout-seconds:15}") int timeoutSeconds
    ) {
        this.ocrMapper = new OcrMapper();
        this.processUrl = processEndpoint;
        this.timeoutSeconds = timeoutSeconds;

        this.webClient = webClientBuilder
                .baseUrl(environmentUrl)
                .defaultHeader("CLIENT-ID", clientId)
                .defaultHeader("AUTHORIZATION", "apikey " + username + ":" + apiKey)
                .build();

        if (!clientSecret.isBlank()) {
            log.debug("Veryfi CLIENT-SECRET is configured");
        }
    }

    @Override
    public OcrResultDto processPrescription(String imageUrl) {
        log.info("Sending OCR request to Veryfi endpoint={} imageUrlHost={}", processUrl, safeHost(imageUrl));

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("file_url", imageUrl);
        requestBody.put("blueprint_name", "medical_prescription_list");

        try {
            String responseBody = webClient.post()
                    .uri(processUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
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

            log.info("Veryfi OCR raw response: {}", responseBody);

                VeryfiOcrResponseDto veryfiResponse = ocrMapper.mapToVeryfiResponse(responseBody);
            log.info("Veryfi OCR response parsed successfully:" + veryfiResponse);
            OcrResultDto result = ocrMapper.map(veryfiResponse);
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
