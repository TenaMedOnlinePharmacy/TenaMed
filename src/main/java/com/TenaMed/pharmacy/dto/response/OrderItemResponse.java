package com.TenaMed.pharmacy.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class OrderItemResponse {

    private UUID id;
    private UUID inventoryId;
    private UUID productId;
    private UUID medicineId;

    private Integer quantity;
    private BigDecimal unitPrice;
}