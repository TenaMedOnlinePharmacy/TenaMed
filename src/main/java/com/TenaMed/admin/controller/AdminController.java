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

import com.TenaMed.hospital.dto.HospitalResponseDto;
import com.TenaMed.pharmacy.dto.response.PharmacyResponse;
import com.TenaMed.medicine.service.MedicineService;
import com.TenaMed.medicine.dto.MedicineRequestDto;
import com.TenaMed.medicine.dto.MedicineResponseDto;
import com.TenaMed.medicine.exception.MedicineAlreadyExistsException;
import com.TenaMed.medicine.exception.MedicineValidationException;
import com.TenaMed.medicine.exception.MedicineNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final PharmacyService pharmacyService;
    private final AdminService adminService;
    private final HospitalService hospitalService;
    private final MedicineService medicineService;

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

    @GetMapping("/hospitals/pending")
    public ResponseEntity<List<HospitalResponseDto>> getPendingHospitals() {
        return ResponseEntity.ok(adminService.getPendingHospitals());
    }

    @GetMapping("/hospitals/search")
    public ResponseEntity<List<HospitalResponseDto>> searchHospitalsByName(@RequestParam String name) {
        return ResponseEntity.ok(adminService.searchHospitalsByName(name));
    }

    @GetMapping("/hospitals/stats")
    public ResponseEntity<Map<String, Long>> getHospitalStats() {
        return ResponseEntity.ok(adminService.getHospitalStats());
    }

    @GetMapping("/pharmacies/pending")
    public ResponseEntity<List<PharmacyResponse>> getPendingPharmacies() {
        return ResponseEntity.ok(adminService.getPendingPharmacies());
    }

    @GetMapping("/pharmacies/search")
    public ResponseEntity<List<PharmacyResponse>> searchPharmaciesByName(@RequestParam String name) {
        return ResponseEntity.ok(adminService.searchPharmaciesByName(name));
    }

    @GetMapping("/pharmacies/stats")
    public ResponseEntity<Map<String, Long>> getPharmacyStats() {
        return ResponseEntity.ok(adminService.getPharmacyStats());
    }

    @PostMapping(value = "/medicines", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createMedicine(@Valid @RequestBody MedicineRequestDto requestDto) {
        try {
            MedicineResponseDto response = medicineService.createMedicine(requestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (MedicineAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (MedicineValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @Deprecated
    @PostMapping(value = "/medicines", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createMedicineWithImage(@Valid @RequestPart("medicine") MedicineRequestDto requestDto,
                                                     @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            MedicineResponseDto response = medicineService.createMedicine(requestDto, image);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (MedicineAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (MedicineValidationException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/medicines/{id}")
    public ResponseEntity<?> updateMedicine(@PathVariable UUID id,
                                            @Valid @RequestBody MedicineRequestDto requestDto) {
        try {
            MedicineResponseDto response = medicineService.updateMedicine(id, requestDto);
            return ResponseEntity.ok(response);
        } catch (MedicineNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (MedicineValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/medicines/{id}")
    public ResponseEntity<?> deleteMedicine(@PathVariable UUID id) {
        try {
            medicineService.deleteMedicine(id);
            return ResponseEntity.noContent().build();
        } catch (MedicineNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
