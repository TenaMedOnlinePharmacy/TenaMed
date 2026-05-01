package com.TenaMed.admin.controller;

import com.TenaMed.admin.dto.DashboardResponse;
import com.TenaMed.admin.dto.OcrStatsResponse;
import com.TenaMed.admin.service.AdminService;
import com.TenaMed.pharmacy.service.PharmacyService;
import com.TenaMed.hospital.service.HospitalService;
import com.TenaMed.audit.entity.AuditLog;
import com.TenaMed.prescription.entity.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final PharmacyService pharmacyService;
    private final AdminService adminService;
    private final HospitalService hospitalService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    @GetMapping("/ocr/stats")
    public ResponseEntity<OcrStatsResponse> getOcrStats() {
        return ResponseEntity.ok(adminService.getOcrStats());
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminService.getAuditLogs(pageable));
    }

    @GetMapping("/prescriptions")
    public ResponseEntity<Page<Prescription>> getPrescriptions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean highRisk,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminService.getPrescriptions(status, highRisk, pageable));
    }

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

    @PostMapping("/hospitals/{id}/approve")
    public ResponseEntity<?> approveHospital(@PathVariable UUID id) {
        hospitalService.verifyHospital(id);
        return ResponseEntity.ok(Map.of("message", "Hospital approved successfully"));
    }

    @PostMapping("/hospitals/{id}/reject")
    public ResponseEntity<?> rejectHospital(@PathVariable UUID id) {
        hospitalService.rejectHospital(id);
        return ResponseEntity.ok(Map.of("message", "Hospital rejected successfully"));
    }

    @PostMapping("/hospitals/{id}/suspend")
    public ResponseEntity<?> suspendHospital(@PathVariable UUID id) {
        hospitalService.suspendHospital(id);
        return ResponseEntity.ok(Map.of("message", "Hospital suspended successfully"));
    }

    @PostMapping("/hospitals/{id}/unsuspend")
    public ResponseEntity<?> unsuspendHospital(@PathVariable UUID id) {
        hospitalService.unsuspendHospital(id);
        return ResponseEntity.ok(Map.of("message", "Hospital unsuspended successfully"));
    }

}
