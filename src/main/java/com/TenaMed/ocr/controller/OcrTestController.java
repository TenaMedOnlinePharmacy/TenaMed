package com.TenaMed.ocr.controller;

import com.TenaMed.Normalization.model.NormalizedOcrResultDto;
import com.TenaMed.ocr.dto.OcrResultDto;
import com.TenaMed.ocr.integration.OcrClient;
import com.TenaMed.ocr.service.OcrService;
import com.TenaMed.ocr.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrTestController {

    private final OcrClient ocrClient;
    private final OcrService ocrService;
    private final SupabaseStorageService supabaseStorageService;

    @PostMapping(value = "/test", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NormalizedOcrResultDto> testOcr(@RequestPart("file") MultipartFile file) {
        String imageUrl = supabaseStorageService.uploadAndGetSignedUrl(file);
        OcrResultDto result = ocrClient.processPrescription(imageUrl);
        NormalizedOcrResultDto normalizedResult = ocrService.processOcrResult(result);
        return ResponseEntity.ok(normalizedResult);
    }
}
