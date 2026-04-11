package com.TenaMed.inventory.dto;

import com.TenaMed.inventory.enums.BatchStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class BatchResponse {

    private UUID id;
    private UUID inventoryId;
    private String batchNumber;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private Integer quantity;
    private BigDecimal unitCost;
    private BigDecimal sellingPrice;
    private BatchStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}