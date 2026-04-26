package com.TenaMed.inventory.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class InventoryResponse {

    private UUID id;
    private UUID pharmacyId;
    private UUID productId;
    private UUID medicineId;
    private Integer totalQuantity;
    private Integer reservedQuantity;
    private Integer reorderLevel;
    private Integer availableQuantity;
    private List<BatchResponse> batches;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}