package com.TenaMed.ocr.controller;

import com.TenaMed.Normalization.model.NormalizedOcrResultDto;
import com.TenaMed.ocr.dto.OcrResultDto;
import com.TenaMed.ocr.integration.OcrClient;
import com.TenaMed.ocr.service.OcrService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ocr")
public class OcrTestController {

    private final OcrClient ocrClient;
    private  final OcrService ocrService;

    public OcrTestController(OcrClient ocrClient, OcrService ocrService) {
        this.ocrClient = ocrClient;
        this.ocrService = ocrService;
    }

    @GetMapping("/test")
    public ResponseEntity<NormalizedOcrResultDto> testOcr() {
        String imageUrl ="https://ytpspmmgeaxouldfeovf.supabase.co/storage/v1/object/sign/book/medical2.jpg?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV84ZmIxZjAzMy1mYjgxLTQwZWItYWVkZC1mNGFiODFkMDk3YjgiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJib29rL21lZGljYWwyLmpwZyIsImlhdCI6MTc3NTcyNDkwNywiZXhwIjoxODA3MjYwOTA3fQ.hxmKp6Iw-zySlg5TBjC-wErbip_LOM4UCuLx1UgjFaI";
        OcrResultDto result = ocrClient.processPrescription(imageUrl);
        NormalizedOcrResultDto normalizedResult = ocrService.processOcrResult(result);
        return ResponseEntity.ok(normalizedResult);
    }
}
