package com.TenaMed.ocr.controller;

import com.TenaMed.common.security.CurrentUserProvider;
import com.TenaMed.ocr.dto.response.OcrUploadResponseDto;
import com.TenaMed.ocr.service.PrescriptionPipelineService;
import com.TenaMed.ocr.service.SupabaseStorageService;
import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrTestController {

    private final SupabaseStorageService supabaseStorageService;
    private final PrescriptionPipelineService prescriptionPipelineService;
    private final PrescriptionService prescriptionService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OcrUploadResponseDto> upload(@RequestPart("file") MultipartFile file){
        String imageUrl = supabaseStorageService.uploadAndGetSignedUrl(file);
        UUID userId = currentUserProvider.getCurrentUserId();
        Prescription prescription = prescriptionService.createUploadedPrescription(userId);
        prescriptionPipelineService.startPipeline(imageUrl, prescription.getId());
        return ResponseEntity.accepted().body(new OcrUploadResponseDto(
                "SUCCESS",
                "Upload successful. Prescription pipeline initialized.",
                prescription.getId()
        ));
    }
}
