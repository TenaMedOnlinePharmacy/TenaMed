package com.TenaMed.inventory.service.impl;

import com.TenaMed.inventory.service.InventoryService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NoOpInventoryService implements InventoryService {

    @Override
    public boolean checkAvailability(UUID pharmacyId, UUID medicineId, Integer quantity) {
        return true;
    }

    @Override
    public boolean reserveStock(UUID pharmacyId, UUID medicineId, Integer quantity) {
        return true;
    }

    @Override
    public void releaseStock(UUID pharmacyId, UUID medicineId, Integer quantity) {
    }
}