package com.TenaMed.cart.service;

import com.TenaMed.cart.dto.request.AddCartItemRequest;
import com.TenaMed.cart.dto.request.UpdateCartItemQuantityRequest;
import com.TenaMed.cart.dto.response.CartResponse;
import com.TenaMed.cart.dto.response.CheckoutCartResponse;

import java.util.UUID;

public interface CartService {

    void ensureActiveCart(UUID userId);

    CartResponse addItem(UUID userId, AddCartItemRequest request);

    CartResponse updateItemQuantity(UUID userId, UUID itemId, UpdateCartItemQuantityRequest request);

    CartResponse removeItem(UUID userId, UUID itemId);

    CartResponse getCart(UUID userId);

    CartResponse clearCart(UUID userId);

    CheckoutCartResponse checkout(UUID userId);
}
