package com.TenaMed.inventory.service;

import com.TenaMed.inventory.dto.CreateInventoryRequest;
import com.TenaMed.inventory.entity.Inventory;
import com.TenaMed.inventory.repository.InventoryRepository;
import com.TenaMed.inventory.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class InventoryReservationConcurrencyTests {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @BeforeEach
    void reset() {
        stockMovementRepository.deleteAll();
        inventoryRepository.deleteAll();
    }

    @Test
    void shouldReserveOnlyAvailableQuantityUnderConcurrentRequests() throws ExecutionException, InterruptedException {
        UUID pharmacyId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();

        CreateInventoryRequest request = new CreateInventoryRequest();
        request.setPharmacyId(pharmacyId);
        request.setMedicineId(medicineId);
        request.setTotalQuantity(10);
        request.setReservedQuantity(0);
        inventoryService.createInventory(request);

        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(() -> inventoryService.reserveStock(pharmacyId, medicineId, 1, UUID.randomUUID()));
        }

        List<Future<Boolean>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        int successCount = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successCount++;
            }
        }

        Inventory persisted = inventoryRepository.findByPharmacyIdAndMedicineId(pharmacyId, medicineId).orElseThrow();
        assertEquals(10, successCount);
        assertEquals(10, persisted.getReservedQuantity());
        assertEquals(10, stockMovementRepository.findByInventoryId(persisted.getId()).size());
    }
}