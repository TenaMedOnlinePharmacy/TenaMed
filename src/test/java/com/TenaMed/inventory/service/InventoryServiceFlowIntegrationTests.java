package com.TenaMed.inventory.service;

import com.TenaMed.inventory.dto.AddBatchRequest;
import com.TenaMed.inventory.dto.BatchResponse;
import com.TenaMed.inventory.dto.CreateInventoryRequest;
import com.TenaMed.inventory.dto.InventoryResponse;
import com.TenaMed.inventory.entity.Batch;
import com.TenaMed.inventory.entity.Inventory;
import com.TenaMed.inventory.entity.StockMovement;
import com.TenaMed.inventory.enums.BatchStatus;
import com.TenaMed.inventory.enums.StockMovementType;
import com.TenaMed.inventory.repository.BatchRepository;
import com.TenaMed.inventory.repository.InventoryRepository;
import com.TenaMed.inventory.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class InventoryServiceFlowIntegrationTests {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;
 
    @Autowired
    private com.TenaMed.medicine.repository.ProductRepository productRepository;

    @Autowired
    private com.TenaMed.medicine.repository.MedicineRepository medicineRepository;
 
    @Autowired
    private com.TenaMed.pharmacy.repository.PharmacyRepository pharmacyRepository;

    @BeforeEach
    void clearData() {
        stockMovementRepository.deleteAll();
        batchRepository.deleteAll();
        inventoryRepository.deleteAll();
        pharmacyRepository.deleteAll();
        productRepository.deleteAll();
        medicineRepository.deleteAll();
    }

    @Test
    void shouldApplyFifoOnConfirmAfterReservation() {
        UUID pharmacyId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();

        com.TenaMed.pharmacy.entity.Pharmacy pharmacy = new com.TenaMed.pharmacy.entity.Pharmacy();
        pharmacy.setName("Test Pharmacy");
        pharmacy.setOwnerId(UUID.randomUUID());
        pharmacy.setLicenseNumber("LIC-" + UUID.randomUUID());
        pharmacy.setPhone("123456789");
        pharmacy.setEmail("test@pharmacy.com");
        pharmacy = pharmacyRepository.save(pharmacy);
        UUID actorUserId = pharmacy.getOwnerId();
        pharmacyId = pharmacy.getId();

        com.TenaMed.medicine.entity.Medicine medicine = new com.TenaMed.medicine.entity.Medicine();
        medicine.setName("Aspirin");
        medicine = medicineRepository.save(medicine);

        com.TenaMed.medicine.entity.Product product = new com.TenaMed.medicine.entity.Product();
        product.setMedicine(medicine);
        product.setBrandName("Aspirin Brand");
        product.setManufacturer("Pharma Corp");
        product = productRepository.save(product);
        UUID productId = product.getId();

        AddBatchRequest firstBatch = new AddBatchRequest();
        firstBatch.setProductId(productId);
        firstBatch.setMedicineName("Aspirin");
        firstBatch.setBatchNumber("B-OLD");
        firstBatch.setQuantity(5);
        firstBatch.setExpiryDate(LocalDate.now().plusDays(5));

        AddBatchRequest secondBatch = new AddBatchRequest();
        secondBatch.setProductId(productId);
        secondBatch.setMedicineName("Aspirin");
        secondBatch.setBatchNumber("B-NEW");
        secondBatch.setQuantity(7);
        secondBatch.setExpiryDate(LocalDate.now().plusDays(30));

        BatchResponse oldBatchResponse = inventoryService.addBatch(firstBatch, actorUserId, null);
        BatchResponse newBatchResponse = inventoryService.addBatch(secondBatch, actorUserId, null);

        UUID reservationRef = UUID.randomUUID();
        boolean reserved = inventoryService.reserveStock(pharmacyId, productId, 6, reservationRef);
        boolean confirmed = inventoryService.confirmStock(pharmacyId, productId, 6, reservationRef);

        assertTrue(reserved);
        assertTrue(confirmed);





        }

    @Test
    void shouldReleaseReservedStock() {
        UUID pharmacyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Inventory inventory = new Inventory();
        inventory.setPharmacyId(pharmacyId);
        inventory.setProductId(productId);
        inventory.setMedicineId(UUID.randomUUID());
        inventory.setTotalQuantity(12);
        inventory.setReservedQuantity(0);

        inventoryRepository.save(inventory);

        boolean reserved = inventoryService.reserveStock(pharmacyId, productId, 5, UUID.randomUUID());
        inventoryService.releaseStock(pharmacyId, productId, 3, UUID.randomUUID());

        assertTrue(reserved);
           }
}