package com.TenaMed.inventory.exception;

import java.util.UUID;

public class InventoryNotFoundException extends InventoryException {

    public InventoryNotFoundException(UUID pharmacyId, UUID medicineId) {
        super("Inventory not found for pharmacy " + pharmacyId + " and medicine " + medicineId);
    }

    public InventoryNotFoundException(UUID inventoryId) {
        super("Inventory not found with id: " + inventoryId);
    }
}