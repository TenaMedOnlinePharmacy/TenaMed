package com.TenaMed.patient.exception;

import java.util.UUID;

public class CustomerAllergyNotFoundException extends PatientException {
    public CustomerAllergyNotFoundException(UUID allergyId) {
        super("Customer allergy not found with id: " + allergyId);
    }
}
