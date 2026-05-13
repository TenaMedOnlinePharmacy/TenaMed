package com.TenaMed.complaint.controller;

import com.TenaMed.complaint.dto.request.AddAdminNoteRequest;
import com.TenaMed.complaint.dto.request.UpdateComplaintStatusRequest;
import com.TenaMed.complaint.dto.response.ComplaintResponse;
import com.TenaMed.complaint.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/admin/complaints")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminComplaintController {

    private final ComplaintService complaintService;

    @GetMapping
    public ResponseEntity<List<ComplaintResponse>> getAllComplaints() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponse> getComplaint(@PathVariable("id") UUID complaintId) {
        return ResponseEntity.ok(complaintService.getComplaint(complaintId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ComplaintResponse> updateStatus(@PathVariable("id") UUID complaintId,
                                                          @Valid @RequestBody UpdateComplaintStatusRequest request) {
        return ResponseEntity.ok(complaintService.updateStatus(complaintId, request.getStatus()));
    }

    @PatchMapping("/{id}/admin-note")
    public ResponseEntity<ComplaintResponse> addAdminNote(@PathVariable("id") UUID complaintId,
                                                          @Valid @RequestBody AddAdminNoteRequest request) {
        return ResponseEntity.ok(complaintService.addAdminNote(complaintId, request.getAdminNote()));
    }
}
