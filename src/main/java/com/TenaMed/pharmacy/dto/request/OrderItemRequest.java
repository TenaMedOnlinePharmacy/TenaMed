package com.TenaMed.pharmacy.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class OrderItemRequest {

    @NotNull
    private UUID inventoryId;

    @NotNull
    private UUID productId;

    @NotNull
    private UUID medicineId;


    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal unitPrice;
}