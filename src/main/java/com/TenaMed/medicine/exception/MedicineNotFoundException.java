package com.TenaMed.medicine.exception;

public class MedicineNotFoundException extends RuntimeException {

    public MedicineNotFoundException(Long id) {
        super("Medicine not found with id: " + id);
    }

    public MedicineNotFoundException(String message) {
        super(message);
    }
}
