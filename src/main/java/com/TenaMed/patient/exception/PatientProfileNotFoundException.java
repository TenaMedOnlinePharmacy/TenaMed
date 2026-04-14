package com.TenaMed.patient.exception;

import java.util.UUID;

public class PatientProfileNotFoundException extends PatientException {
    public PatientProfileNotFoundException(UUID userId) {
        super("Patient profile not found for userId: " + userId);
    }
}
