package com.TenaMed.inventory.mapper;

import com.TenaMed.inventory.dto.BatchResponse;
import com.TenaMed.inventory.dto.CreateInventoryRequest;
import com.TenaMed.inventory.dto.InventoryResponse;
import com.TenaMed.inventory.entity.Inventory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryMapper {

    public Inventory toEntity(CreateInventoryRequest request) {
        Inventory inventory = new Inventory();
        inventory.setPharmacyId(request.getPharmacyId());
        inventory.setMedicineId(request.getMedicineId());
        inventory.setTotalQuantity(request.getTotalQuantity());
        inventory.setReservedQuantity(request.getReservedQuantity() == null ? 0 : request.getReservedQuantity());
        inventory.setReorderLevel(request.getReorderLevel());
        return inventory;
    }

    public InventoryResponse toResponse(Inventory inventory, List<BatchResponse> batches) {
        int available = inventory.getTotalQuantity() - inventory.getReservedQuantity();
        return InventoryResponse.builder()
            .id(inventory.getId())
            .pharmacyId(inventory.getPharmacyId())
            .medicineId(inventory.getMedicineId())
            .totalQuantity(inventory.getTotalQuantity())
            .reservedQuantity(inventory.getReservedQuantity())
            .reorderLevel(inventory.getReorderLevel())
            .availableQuantity(available)
            .batches(batches)
            .createdAt(inventory.getCreatedAt())
            .updatedAt(inventory.getUpdatedAt())
            .build();
    }
}