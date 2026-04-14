package com.TenaMed.patient.exception;

public class DuplicateAllergyException extends PatientException {
    public DuplicateAllergyException() {
        super("Allergy already exists for this profile and allergen");
    }
}
