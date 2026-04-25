package com.TenaMed.pharmacy.controller;

import com.TenaMed.common.security.CurrentUserProvider;
import com.TenaMed.pharmacy.dto.request.AddStaffRequest;
import com.TenaMed.pharmacy.dto.response.StaffResponse;
import com.TenaMed.pharmacy.exception.PharmacyException;
import com.TenaMed.pharmacy.exception.PharmacyNotFoundException;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
import com.TenaMed.pharmacy.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/pharmacies")
public class StaffController {

    private final StaffService staffService;
    private final CurrentUserProvider currentUserProvider;
    private final PharmacyRepository pharmacyRepository;

    public StaffController(StaffService staffService,
                           CurrentUserProvider currentUserProvider,
                           PharmacyRepository pharmacyRepository) {
        this.staffService = staffService;
        this.currentUserProvider = currentUserProvider;
        this.pharmacyRepository = pharmacyRepository;
    }

    @PostMapping("/{id}/staff")
    public ResponseEntity<?> addStaff(@PathVariable UUID id,
                                      @Valid @RequestBody AddStaffRequest request) {
        try {
            request.setPharmacyId(id);
            StaffResponse response = staffService.addStaff(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (PharmacyException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/staff")
    public ResponseEntity<?> listStaff() {
        try {
            UUID ownerId = currentUserProvider.getCurrentUserId();
            UUID pharmacyId = pharmacyRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new PharmacyNotFoundException("Pharmacy not found for current owner"))
                .getId();
            return ResponseEntity.ok(staffService.listStaff(pharmacyId));
        } catch (PharmacyException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/staff/{userId}/verify")
    public ResponseEntity<?> verifyPharmacist(@PathVariable UUID userId) {
        try {
            UUID ownerId = currentUserProvider.getCurrentUserId();
            UUID pharmacyId = pharmacyRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new PharmacyNotFoundException("Pharmacy not found for current owner"))
                .getId();
            StaffResponse response = staffService.verifyStaff(pharmacyId, userId, ownerId);
            return ResponseEntity.ok(response);
        } catch (PharmacyException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }
}