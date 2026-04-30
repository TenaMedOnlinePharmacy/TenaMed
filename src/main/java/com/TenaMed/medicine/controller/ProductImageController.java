package com.TenaMed.medicine.controller;

import com.TenaMed.medicine.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService productImageService;

    @PostMapping(value = "/{productId}/images/default", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDefaultImage(
            @PathVariable UUID productId,
            @RequestPart("image") MultipartFile image) {
        try {
            String imageUrl = productImageService.uploadDefaultImage(productId, image);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("imageUrl", imageUrl, "type", "default"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/{productId}/images/pharmacy/{pharmacyId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPharmacyImage(
            @PathVariable UUID productId,
            @PathVariable UUID pharmacyId,
            @RequestPart("image") MultipartFile image) {
        try {
            String imageUrl = productImageService.uploadPharmacyImage(productId, pharmacyId, image);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("imageUrl", imageUrl, "type", "pharmacy", "pharmacyId", pharmacyId.toString()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{productId}/image")
    public ResponseEntity<?> resolveImage(
            @PathVariable UUID productId,
            @RequestParam(required = false) UUID pharmacyId) {
        String imageUrl = productImageService.resolveProductImage(productId, pharmacyId);
        if (imageUrl == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }
}
