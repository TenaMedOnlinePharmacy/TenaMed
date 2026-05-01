package com.tenamed.admin.controller;

import com.TenaMed.pharmacy.service.PharmacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final PharmacyService pharmacyService;

    @PostMapping("/pharmacies/{id}/approve")
    public ResponseEntity<?> approvePharmacy(@PathVariable UUID id) {
        pharmacyService.verifyPharmacy(id);
        return ResponseEntity.ok(Map.of("message", "Pharmacy approved successfully"));
    }

    @PostMapping("/pharmacies/{id}/reject")
    public ResponseEntity<?> rejectPharmacy(@PathVariable UUID id) {
        pharmacyService.rejectPharmacy(id);
        return ResponseEntity.ok(Map.of("message", "Pharmacy rejected successfully"));
    }

    @PostMapping("/pharmacies/{id}/suspend")
    public ResponseEntity<?> suspendPharmacy(@PathVariable UUID id) {
        pharmacyService.suspendPharmacy(id);
        return ResponseEntity.ok(Map.of("message", "Pharmacy suspended successfully"));
    }

    @PostMapping("/pharmacies/{id}/unsuspend")
    public ResponseEntity<?> unsuspendPharmacy(@PathVariable UUID id) {
        pharmacyService.unsuspendPharmacy(id);
        return ResponseEntity.ok(Map.of("message", "Pharmacy unsuspended successfully"));
    }
}
