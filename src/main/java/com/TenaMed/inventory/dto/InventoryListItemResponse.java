package com.TenaMed.inventory.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class InventoryListItemResponse {

    private UUID inventoryId;
    private UUID productId;
    private String imageUrl;
    private String medicineName;
    private Integer totalQuantity;
    private List<BatchPriceResponse> batchPrices;
    private String brand;
    private String manufacturer;
    private Integer remainingQuantity;

    @Getter
    @Builder
    public static class BatchPriceResponse {
        private UUID batchId;
        private BigDecimal unitPrice;
        private BigDecimal sellingPrice;
    }
}
