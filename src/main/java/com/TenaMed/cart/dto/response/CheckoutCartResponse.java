package com.TenaMed.cart.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class CheckoutCartResponse {
    List<UUID> orderIds;
    String message;
    String status;
}
