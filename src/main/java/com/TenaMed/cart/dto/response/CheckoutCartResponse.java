package com.TenaMed.cart.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class CheckoutCartResponse {
    UUID orderId;
    String message;
    String status;
}
