package com.TenaMed.patient.exception;

import java.util.UUID;

public class PatientProfileAlreadyExistsException extends PatientException {
    public PatientProfileAlreadyExistsException(UUID userId) {
        super("Patient profile already exists for userId: " + userId);
    }
}
