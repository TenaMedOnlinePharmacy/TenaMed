package com.TenaMed.complaint.controller;

import com.TenaMed.complaint.dto.request.CreateComplaintRequest;
import com.TenaMed.complaint.dto.response.ComplaintResponse;
import com.TenaMed.complaint.service.ComplaintService;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    public ResponseEntity<?> createComplaint(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                             @Valid @RequestBody CreateComplaintRequest request) {
        UUID customerId = resolveUserId(principal);
        if (customerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        ComplaintResponse response = complaintService.createComplaint(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyComplaints(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        UUID customerId = resolveUserId(principal);
        if (customerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        List<ComplaintResponse> response = complaintService.getCustomerComplaints(customerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getComplaint(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                          @PathVariable("id") UUID complaintId) {
        UUID customerId = resolveUserId(principal);
        if (customerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        ComplaintResponse response = complaintService.getCustomerComplaint(customerId, complaintId);
        return ResponseEntity.ok(response);
    }

    private UUID resolveUserId(AuthenticatedUserPrincipal principal) {
        return principal == null ? null : principal.getUserId();
    }
}
