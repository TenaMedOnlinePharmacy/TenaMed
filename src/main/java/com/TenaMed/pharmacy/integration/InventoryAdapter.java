package com.TenaMed.pharmacy.integration;

import com.TenaMed.inventory.service.InventoryService;
import com.TenaMed.pharmacy.exception.ExternalModuleException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InventoryAdapter {

    private final InventoryService inventoryService;

    public InventoryAdapter(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public boolean checkAvailability(UUID pharmacyId, UUID medicineId, Integer quantity) {
        try {
            return inventoryService.checkAvailability(pharmacyId, medicineId, quantity);
        } catch (Exception ex) {
            throw new ExternalModuleException("Failed to check inventory availability", ex);
        }
    }

    public boolean reserveStock(UUID pharmacyId, UUID medicineId, Integer quantity) {
        try {
            return inventoryService.reserveStock(pharmacyId, medicineId, quantity);
        } catch (Exception ex) {
            throw new ExternalModuleException("Failed to reserve inventory stock", ex);
        }
    }

    public void releaseStock(UUID pharmacyId, UUID medicineId, Integer quantity) {
        try {
            inventoryService.releaseStock(pharmacyId, medicineId, quantity);
        } catch (Exception ex) {
            throw new ExternalModuleException("Failed to release inventory stock", ex);
        }
    }
}