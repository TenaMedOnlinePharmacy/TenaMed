package com.TenaMed.patient.exception;

public class DuplicateUniqueCodeException extends PatientException {

    public DuplicateUniqueCodeException(String uniqueCode) {
        super("Patient unique code already exists: " + uniqueCode);
    }
}
