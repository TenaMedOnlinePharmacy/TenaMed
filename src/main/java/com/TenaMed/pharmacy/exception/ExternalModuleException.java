package com.TenaMed.pharmacy.exception;

public class ExternalModuleException extends PharmacyException {

    public ExternalModuleException(String message, Throwable cause) {
        super(message + ": " + cause.getMessage());
    }
}