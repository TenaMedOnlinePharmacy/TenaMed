package com.TenaMed.inventory.exception;

import java.util.UUID;

public class DuplicateInventoryException extends InventoryException {

    public DuplicateInventoryException(UUID pharmacyId, UUID medicineId) {
        super("Inventory already exists for pharmacy " + pharmacyId + " and medicine " + medicineId);
    }
}