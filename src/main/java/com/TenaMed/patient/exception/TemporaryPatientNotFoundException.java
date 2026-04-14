package com.TenaMed.patient.exception;

import java.util.UUID;

public class TemporaryPatientNotFoundException extends PatientException {
    public TemporaryPatientNotFoundException(UUID patientId) {
        super("Temporary patient not found with id: " + patientId);
    }
}
