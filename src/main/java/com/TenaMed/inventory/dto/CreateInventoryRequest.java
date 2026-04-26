package com.TenaMed.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateInventoryRequest {

    @NotNull
    private UUID pharmacyId;

    @NotNull
    private UUID productId;

    @NotNull
    @Min(0)
    private Integer totalQuantity;

    @Min(0)
    private Integer reservedQuantity;

    @Min(0)
    private Integer reorderLevel;
}