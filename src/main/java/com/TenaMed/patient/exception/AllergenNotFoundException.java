package com.TenaMed.patient.exception;

import java.util.UUID;

public class AllergenNotFoundException extends PatientException {
    public AllergenNotFoundException(UUID allergenId) {
        super("Allergen not found with id: " + allergenId);
    }
}
