package com.TenaMed.pharmacy.controller;

import com.TenaMed.pharmacy.dto.request.AddStaffRequest;
import com.TenaMed.pharmacy.dto.response.StaffResponse;
import com.TenaMed.pharmacy.exception.PharmacyException;
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

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
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

    @GetMapping("/{id}/staff")
    public ResponseEntity<List<StaffResponse>> listStaff(@PathVariable UUID id) {
        return ResponseEntity.ok(staffService.listStaff(id));
    }
}