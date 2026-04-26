package com.TenaMed.inventory.service;

import com.TenaMed.inventory.dto.AddBatchRequest;
import com.TenaMed.inventory.dto.BatchResponse;
import com.TenaMed.inventory.dto.CreateInventoryRequest;
import com.TenaMed.inventory.dto.InventoryResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface InventoryService {

    InventoryResponse createInventory(CreateInventoryRequest request);

    BatchResponse addBatch(AddBatchRequest request, UUID actorUserId);

    InventoryResponse getInventory(UUID pharmacyId, UUID medicineId);

    boolean checkAvailability(UUID pharmacyId, UUID medicineId, Integer quantity);

    boolean checkAvailability(UUID medicineId, Integer quantity);

    List<UUID> findPharmacyIdsWithAvailableMedicine(UUID medicineId, Integer quantity);

    BigDecimal resolveUnitPrice(UUID medicineId);

    boolean reserveStock(UUID pharmacyId, UUID medicineId, Integer quantity);

    boolean reserveStock(UUID pharmacyId, UUID medicineId, Integer quantity, UUID referenceId);

    boolean confirmStock(UUID pharmacyId, UUID medicineId, Integer quantity, UUID referenceId);

    void releaseStock(UUID pharmacyId, UUID medicineId, Integer quantity);

    void releaseStock(UUID pharmacyId, UUID medicineId, Integer quantity, UUID referenceId);
}