package com.TenaMed.verification.exception;

import java.util.UUID;

public class PrescriptionNotFoundException extends VerificationException {

    public PrescriptionNotFoundException(UUID id) {
        super("Prescription not found with id: " + id);
    }
}
