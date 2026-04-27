package com.TenaMed.inventory.exception;

import java.util.UUID;

public class BatchNotFoundException extends InventoryException {

    public BatchNotFoundException(UUID batchId) {
        super("Batch not found with id: " + batchId);
    }
}
