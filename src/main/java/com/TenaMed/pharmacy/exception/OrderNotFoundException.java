package com.TenaMed.pharmacy.exception;

import java.util.UUID;

public class OrderNotFoundException extends PharmacyException {

    public OrderNotFoundException(UUID orderId) {
        super("Order not found with id: " + orderId);
    }
}