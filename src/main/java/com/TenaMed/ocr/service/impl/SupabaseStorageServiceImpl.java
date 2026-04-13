package com.TenaMed.ocr.service.impl;

import com.TenaMed.ocr.service.SupabaseStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
public class SupabaseStorageServiceImpl implements SupabaseStorageService {

    private final WebClient webClient;
    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final String bucket;
    private final int signedUrlExpirySeconds;

    public SupabaseStorageServiceImpl(
            WebClient.Builder webClientBuilder,
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.service-role-key}") String serviceRoleKey,
            @Value("${supabase.storage.bucket}") String bucket,
            @Value("${supabase.storage.signed-url-expiry-seconds:600}") int signedUrlExpirySeconds
    ) {
        this.webClient = webClientBuilder.build();
        this.supabaseUrl = trimTrailingSlash(supabaseUrl);
        this.serviceRoleKey = serviceRoleKey;
        this.bucket = bucket;
        this.signedUrlExpirySeconds = signedUrlExpirySeconds;
    }

    @Override
    public String uploadAndGetSignedUrl(MultipartFile file) {
        validate(file);

        String objectPath = buildObjectPath(file.getOriginalFilename());
        uploadBinary(file, objectPath);

        return createSignedUrl(objectPath);
    }

    private void uploadBinary(MultipartFile file, String objectPath) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read uploaded file", ex);
        }

        String contentType = file.getContentType() != null
                ? file.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        webClient.post()
                .uri(supabaseUrl + "/storage/v1/object/" + bucket + "/" + objectPath)
                .header("apikey", serviceRoleKey)
                .header("Authorization", "Bearer " + serviceRoleKey)
                .header("x-upsert", "true")
                .contentType(MediaType.parseMediaType(contentType))
                .bodyValue(content)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private String createSignedUrl(String objectPath) {
        @SuppressWarnings("unchecked")
        Map<String, Object> response = webClient.post()
                .uri(supabaseUrl + "/storage/v1/object/sign/" + bucket + "/" + objectPath)
                .header("apikey", serviceRoleKey)
                .header("Authorization", "Bearer " + serviceRoleKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("expiresIn", signedUrlExpirySeconds))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || response.get("signedURL") == null) {
            throw new IllegalStateException("Failed to create Supabase signed URL");
        }

        return supabaseUrl + "/storage/v1" + response.get("signedURL").toString();
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        if (serviceRoleKey == null || serviceRoleKey.isBlank()) {
            throw new IllegalStateException("supabase.service-role-key is not configured");
        }
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("supabase.storage.bucket is not configured");
        }
    }

    private String buildObjectPath(String originalName) {
        String fileName = (originalName == null || originalName.isBlank()) ? "upload.bin" : originalName;
        String safeName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "ocr/" + LocalDate.now() + "/" + UUID.randomUUID() + "-" + safeName;
    }

    private String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("supabase.url is not configured");
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
