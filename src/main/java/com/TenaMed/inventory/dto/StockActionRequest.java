package com.TenaMed.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class StockActionRequest {

    @NotNull
    private UUID pharmacyId;

    @NotNull
    private UUID medicineId;

    @NotNull
    @Min(1)
    private Integer quantity;

    private UUID referenceId;
}