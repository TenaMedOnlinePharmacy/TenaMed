package com.TenaMed.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class AddBatchRequest {

    @NotNull
    private String medicineName;

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

    private Integer reorderLevel;
}