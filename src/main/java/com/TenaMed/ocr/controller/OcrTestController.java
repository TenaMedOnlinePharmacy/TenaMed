package com.TenaMed.ocr.controller;

import com.TenaMed.ocr.dto.OcrResultDto;
import com.TenaMed.ocr.integration.OcrClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ocr")
public class OcrTestController {

    private final OcrClient ocrClient;

    public OcrTestController(OcrClient ocrClient) {
        this.ocrClient = ocrClient;
    }

    @GetMapping("/test")
    public ResponseEntity<OcrResultDto> testOcr() {
        String imageUrl ="https://ytpspmmgeaxouldfeovf.supabase.co/storage/v1/object/sign/book/medical2.jpg?token=eyJraWQiOiJzdG9yYWdlLXVybC1zaWduaW5nLWtleV84ZmIxZjAzMy1mYjgxLTQwZWItYWVkZC1mNGFiODFkMDk3YjgiLCJhbGciOiJIUzI1NiJ9.eyJ1cmwiOiJib29rL21lZGljYWwyLmpwZyIsImlhdCI6MTc3NTExMTI0MCwiZXhwIjoxNzc1NzE2MDQwfQ.WWwWCYm59RwtVSpRVn_q98rSNvQMQtYgj0d4HZTYXt4";
                OcrResultDto result = ocrClient.processPrescription(imageUrl);
        return ResponseEntity.ok(result);
    }
}
