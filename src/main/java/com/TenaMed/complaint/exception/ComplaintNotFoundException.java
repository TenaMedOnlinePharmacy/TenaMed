package com.TenaMed.complaint.exception;

import java.util.UUID;

public class ComplaintNotFoundException extends ComplaintException {

    public ComplaintNotFoundException(UUID complaintId) {
        super("Complaint not found with id: " + complaintId);
    }
}
