package com.TenaMed.pharmacy.exception;

import java.util.UUID;

public class PrescriptionValidationException extends PharmacyException {

    public PrescriptionValidationException(UUID prescriptionId) {
        super("Prescription not found with id: " + prescriptionId);
    }
}