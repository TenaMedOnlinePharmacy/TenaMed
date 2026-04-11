package com.TenaMed.inventory.entity;

import com.TenaMed.inventory.enums.BatchStatus;
import com.TenaMed.inventory.enums.StockMovementType;
import com.TenaMed.inventory.repository.BatchRepository;
import com.TenaMed.inventory.repository.InventoryRepository;
import com.TenaMed.inventory.repository.StockMovementRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
class InventoryEntityPersistenceTests {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Test
    void shouldPersistInventoryBatchAndStockMovement() {
        Inventory inventory = new Inventory();
        inventory.setPharmacyId(UUID.randomUUID());
        inventory.setMedicineId(UUID.randomUUID());
        inventory.setTotalQuantity(100);
        inventory.setReservedQuantity(10);
        inventory.setReorderLevel(20);
        Inventory savedInventory = inventoryRepository.save(inventory);

        Batch batch = new Batch();
        batch.setInventory(savedInventory);
        batch.setBatchNumber("B-100");
        batch.setManufacturingDate(LocalDate.now().minusMonths(1));
        batch.setExpiryDate(LocalDate.now().plusMonths(6));
        batch.setQuantity(100);
        batch.setUnitCost(new BigDecimal("9.50"));
        batch.setSellingPrice(new BigDecimal("12.75"));
        batch.setStatus(BatchStatus.ACTIVE);
        Batch savedBatch = batchRepository.save(batch);

        StockMovement movement = new StockMovement();
        movement.setInventoryId(savedInventory.getId());
        movement.setType(StockMovementType.IN);
        movement.setQuantity(100);
        movement.setReferenceId(UUID.randomUUID());
        StockMovement savedMovement = stockMovementRepository.save(movement);

        assertNotNull(savedInventory.getId());
        assertNotNull(savedBatch.getId());
        assertNotNull(savedMovement.getId());
        assertEquals(savedInventory.getId(), savedBatch.getInventory().getId());
        assertEquals(savedInventory.getId(), savedMovement.getInventoryId());
    }
}