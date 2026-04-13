package com.TenaMed.cart.dto.response;

import com.TenaMed.cart.entity.CartStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class CartResponse {
    UUID id;
    UUID userId;
    CartStatus status;
    LocalDateTime expiresAt;
    List<CartItemResponse> items;
    BigDecimal cartTotal;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
