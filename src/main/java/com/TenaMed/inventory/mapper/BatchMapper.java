package com.TenaMed.inventory.mapper;

import com.TenaMed.inventory.dto.AddBatchRequest;
import com.TenaMed.inventory.dto.BatchResponse;
import com.TenaMed.inventory.entity.Batch;
import com.TenaMed.inventory.entity.Inventory;
import com.TenaMed.inventory.enums.BatchStatus;
import org.springframework.stereotype.Component;

@Component
public class BatchMapper {

    public Batch toEntity(AddBatchRequest request, Inventory inventory) {
        Batch batch = new Batch();
        batch.setInventory(inventory);
        batch.setBatchNumber(request.getBatchNumber());
        batch.setManufacturingDate(request.getManufacturingDate());
        batch.setExpiryDate(request.getExpiryDate());
        batch.setQuantity(request.getQuantity());
        batch.setUnitCost(request.getUnitCost());
        batch.setSellingPrice(request.getSellingPrice());
        batch.setStatus(request.getStatus() == null ? BatchStatus.ACTIVE : request.getStatus());
        return batch;
    }

    public BatchResponse toResponse(Batch batch) {
        return BatchResponse.builder()
            .id(batch.getId())
            .inventoryId(batch.getInventory().getId())
            .batchNumber(batch.getBatchNumber())
            .manufacturingDate(batch.getManufacturingDate())
            .expiryDate(batch.getExpiryDate())
            .quantity(batch.getQuantity())
            .unitCost(batch.getUnitCost())
            .sellingPrice(batch.getSellingPrice())
            .status(batch.getStatus())
            .createdAt(batch.getCreatedAt())
            .updatedAt(batch.getUpdatedAt())
            .build();
    }
}