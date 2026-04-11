package com.TenaMed.inventory.service;

import java.util.UUID;

public interface InventoryService {

    boolean checkAvailability(UUID pharmacyId, UUID medicineId, Integer quantity);

    boolean reserveStock(UUID pharmacyId, UUID medicineId, Integer quantity);

    void releaseStock(UUID pharmacyId, UUID medicineId, Integer quantity);
}