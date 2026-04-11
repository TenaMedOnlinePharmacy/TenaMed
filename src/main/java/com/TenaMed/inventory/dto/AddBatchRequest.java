package com.TenaMed.inventory.dto;

import com.TenaMed.inventory.enums.BatchStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class AddBatchRequest {

    @NotNull
    private UUID inventoryId;

    private String batchNumber;

    private LocalDate manufacturingDate;

    private LocalDate expiryDate;

    @NotNull
    @Min(1)
    private Integer quantity;

    @DecimalMin("0.0")
    private BigDecimal unitCost;

    @DecimalMin("0.0")
    private BigDecimal sellingPrice;

    private BatchStatus status;
}