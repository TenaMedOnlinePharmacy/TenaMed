package com.TenaMed.pharmacy.controller;

import com.TenaMed.common.security.CurrentUserProvider;
import com.TenaMed.pharmacy.dto.request.CreatePharmacyRequest;
import com.TenaMed.pharmacy.dto.response.PharmacyResponse;
import com.TenaMed.invitation.dto.InvitationResponseDto;
import com.TenaMed.invitation.dto.PharmacistInvitationRequestDto;
import com.TenaMed.pharmacy.exception.PharmacyException;
import com.TenaMed.pharmacy.exception.PharmacyNotFoundException;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
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
    private final CurrentUserProvider currentUserProvider;
    private final PharmacyRepository pharmacyRepository;

    public PharmacyController(PharmacyService pharmacyService,
                              CurrentUserProvider currentUserProvider,
                              PharmacyRepository pharmacyRepository) {
        this.pharmacyService = pharmacyService;
        this.currentUserProvider = currentUserProvider;
        this.pharmacyRepository = pharmacyRepository;
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


    @PostMapping("/invite-pharmacist")
    public ResponseEntity<?> invitePharmacist(@Valid @RequestBody PharmacistInvitationRequestDto request) {
        try {
            UUID ownerId = currentUserProvider.getCurrentUserId();
            UUID pharmacyId = pharmacyRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new PharmacyNotFoundException("Pharmacy not found for current owner"))
                .getId();
            InvitationResponseDto response = pharmacyService.invitePharmacist(pharmacyId, request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (PharmacyException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }
}