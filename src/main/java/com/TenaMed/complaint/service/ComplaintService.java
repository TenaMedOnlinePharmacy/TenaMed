package com.TenaMed.complaint.service;

import com.TenaMed.complaint.dto.request.CreateComplaintRequest;
import com.TenaMed.complaint.dto.response.ComplaintResponse;
import com.TenaMed.complaint.enums.ComplaintStatus;

import java.util.List;
import java.util.UUID;

public interface ComplaintService {

    ComplaintResponse createComplaint(UUID customerId, CreateComplaintRequest request);

    List<ComplaintResponse> getCustomerComplaints(UUID customerId);

    ComplaintResponse getCustomerComplaint(UUID customerId, UUID complaintId);

    List<ComplaintResponse> getAllComplaints();

    ComplaintResponse getComplaint(UUID complaintId);

    ComplaintResponse updateStatus(UUID complaintId, ComplaintStatus status);

    ComplaintResponse addAdminNote(UUID complaintId, String adminNote);
}
