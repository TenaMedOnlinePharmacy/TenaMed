package com.TenaMed.cart.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class CartItemResponse {
    UUID id;
    UUID medicineId;
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal totalPrice;
    Boolean requiresPrescription;
    UUID prescriptionId;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
