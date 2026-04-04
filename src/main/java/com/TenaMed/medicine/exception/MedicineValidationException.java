package com.TenaMed.medicine.exception;

import java.util.List;

public class MedicineValidationException extends RuntimeException {

    private final List<String> errors;

    public MedicineValidationException(List<String> errors) {
        super("Medicine validation failed: " + String.join(", ", errors));
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
