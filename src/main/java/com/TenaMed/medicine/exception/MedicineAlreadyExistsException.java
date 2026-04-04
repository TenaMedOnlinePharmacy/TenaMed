package com.TenaMed.medicine.exception;

public class MedicineAlreadyExistsException extends RuntimeException {

    public MedicineAlreadyExistsException(String name) {
        super("Medicine already exists with name: " + name);
    }
}
