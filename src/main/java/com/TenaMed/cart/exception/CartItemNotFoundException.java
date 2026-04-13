package com.TenaMed.cart.exception;

import java.util.UUID;

public class CartItemNotFoundException extends CartException {
    public CartItemNotFoundException(UUID itemId) {
        super("Cart item not found with id: " + itemId);
    }
}
