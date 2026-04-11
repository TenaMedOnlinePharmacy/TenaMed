package com.TenaMed.inventory.repository;

import com.TenaMed.inventory.entity.Batch;
import com.TenaMed.inventory.entity.Inventory;
import com.TenaMed.inventory.enums.BatchStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class InventoryRepositoryDataJpaTests {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Test
    void shouldFindInventoryByPharmacyAndMedicine() {
        UUID pharmacyId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();

        Inventory inventory = new Inventory();
        inventory.setPharmacyId(pharmacyId);
        inventory.setMedicineId(medicineId);
        inventory.setTotalQuantity(50);
        inventory.setReservedQuantity(5);
        inventory.setReorderLevel(10);
        inventoryRepository.save(inventory);

        Optional<Inventory> found = inventoryRepository.findByPharmacyIdAndMedicineId(pharmacyId, medicineId);
        assertTrue(found.isPresent());
        assertEquals(50, found.get().getTotalQuantity());
    }

    @Test
    void shouldFindBatchesOrderedByExpiryDate() {
        Inventory inventory = new Inventory();
        inventory.setPharmacyId(UUID.randomUUID());
        inventory.setMedicineId(UUID.randomUUID());
        inventory.setTotalQuantity(120);
        inventory.setReservedQuantity(0);
        inventory.setReorderLevel(10);
        Inventory savedInventory = inventoryRepository.save(inventory);

        Batch later = new Batch();
        later.setInventory(savedInventory);
        later.setBatchNumber("B-LATE");
        later.setExpiryDate(LocalDate.now().plusMonths(12));
        later.setQuantity(40);
        later.setStatus(BatchStatus.ACTIVE);

        Batch sooner = new Batch();
        sooner.setInventory(savedInventory);
        sooner.setBatchNumber("B-SOON");
        sooner.setExpiryDate(LocalDate.now().plusMonths(2));
        sooner.setQuantity(30);
        sooner.setStatus(BatchStatus.ACTIVE);

        batchRepository.saveAll(List.of(later, sooner));

        List<Batch> ordered = batchRepository.findByInventoryIdOrderByExpiryDateAsc(savedInventory.getId());
        assertEquals(2, ordered.size());
        assertEquals("B-SOON", ordered.getFirst().getBatchNumber());
    }
}