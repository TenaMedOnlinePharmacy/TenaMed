package com.TenaMed.pharmacy.exception;

public class OrderAuthorizationException extends PharmacyException {

    public OrderAuthorizationException() {
        super("Only pharmacy owner or pharmacist can accept orders");
    }
}