package com.TenaMed.pharmacy.controller;

import com.TenaMed.pharmacy.dto.request.CreatePharmacyRequest;
import com.TenaMed.pharmacy.dto.response.PharmacyResponse;
import com.TenaMed.invitation.dto.InvitationResponseDto;
import com.TenaMed.invitation.dto.PharmacistInvitationRequestDto;
import com.TenaMed.pharmacy.exception.PharmacyException;
import com.TenaMed.pharmacy.service.PharmacyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/pharmacies")
public class PharmacyController {

    private final PharmacyService pharmacyService;

    public PharmacyController(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    @PostMapping
    public ResponseEntity<?> createPharmacy(@Valid @RequestBody CreatePharmacyRequest request) {
        try {
            PharmacyResponse response = pharmacyService.createPharmacy(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (PharmacyException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPharmacy(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(pharmacyService.getPharmacy(id));
        } catch (PharmacyException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<?> verifyPharmacy(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(pharmacyService.verifyPharmacy(id));
        } catch (PharmacyException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/invite-pharmacist")
    public ResponseEntity<?> invitePharmacist(@PathVariable UUID id,
                                              @Valid @RequestBody PharmacistInvitationRequestDto request) {
        try {
            InvitationResponseDto response = pharmacyService.invitePharmacist(id, request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (PharmacyException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }
}