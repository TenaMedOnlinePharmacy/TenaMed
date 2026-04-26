package com.TenaMed.inventory.service;

import com.TenaMed.inventory.dto.AddBatchRequest;
import com.TenaMed.inventory.dto.CreateInventoryRequest;
import com.TenaMed.inventory.entity.Batch;
import com.TenaMed.inventory.entity.Inventory;
import com.TenaMed.inventory.enums.BatchStatus;
import com.TenaMed.inventory.exception.DuplicateInventoryException;
import com.TenaMed.inventory.exception.InventoryValidationException;
import com.TenaMed.inventory.mapper.BatchMapper;
import com.TenaMed.inventory.mapper.InventoryMapper;
import com.TenaMed.inventory.repository.BatchRepository;
import com.TenaMed.inventory.repository.InventoryRepository;
import com.TenaMed.inventory.repository.StockMovementRepository;
import com.TenaMed.inventory.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTests {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private BatchMapper batchMapper;

    @Mock
    private DomainEventService domainEventService;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private PharmacyRepository pharmacyRepository;

    @Mock
    private UserPharmacyRepository userPharmacyRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Test
    void shouldRejectDuplicateInventory() {
        UUID pharmacyId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();

        CreateInventoryRequest request = new CreateInventoryRequest();
        request.setPharmacyId(pharmacyId);
        request.setMedicineId(medicineId);
        request.setTotalQuantity(10);

        when(inventoryRepository.findByPharmacyIdAndMedicineId(pharmacyId, medicineId))
            .thenReturn(Optional.of(new Inventory()));

        assertThrows(DuplicateInventoryException.class, () -> inventoryService.createInventory(request));
    }

    @Test
    void shouldAddBatchAndIncreaseInventoryTotal() {
        UUID actorUserId = UUID.randomUUID();
        UUID pharmacyId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        String medicineName = "Aspirin";

        com.TenaMed.pharmacy.entity.Pharmacy pharmacy = new com.TenaMed.pharmacy.entity.Pharmacy();
        pharmacy.setId(pharmacyId);

        com.TenaMed.medicine.entity.Medicine medicine = new com.TenaMed.medicine.entity.Medicine();
        medicine.setId(medicineId);
        medicine.setName(medicineName);

        Inventory inventory = new Inventory();
        inventory.setId(UUID.randomUUID());
        inventory.setPharmacyId(pharmacyId);
        inventory.setMedicineId(medicineId);
        inventory.setTotalQuantity(5);
        inventory.setReservedQuantity(0);

        AddBatchRequest request = new AddBatchRequest();
        request.setMedicineName(medicineName);
        request.setQuantity(7);

        Batch batch = new Batch();
        batch.setId(UUID.randomUUID());
        batch.setInventory(inventory);
        batch.setQuantity(7);
        batch.setStatus(BatchStatus.ACTIVE);

        when(pharmacyRepository.findByOwnerId(actorUserId)).thenReturn(Optional.of(pharmacy));
        when(medicineRepository.findByNameIgnoreCase(medicineName)).thenReturn(Optional.of(medicine));
        when(inventoryRepository.findByPharmacyIdAndMedicineId(pharmacyId, medicineId)).thenReturn(Optional.of(inventory));
        when(batchMapper.toEntity(request, inventory)).thenReturn(batch);
        when(batchRepository.save(batch)).thenReturn(batch);

        inventoryService.addBatch(request, actorUserId);

        assertEquals(12, inventory.getTotalQuantity());
        verify(inventoryRepository).save(inventory);
        verify(stockMovementRepository).save(any());
    }

    @Test
    void shouldReturnFalseWhenReserveQuantityExceedsAvailability() {
        Inventory inventory = new Inventory();
        inventory.setTotalQuantity(3);
        inventory.setReservedQuantity(1);

        when(inventoryRepository.findWithLockByPharmacyIdAndMedicineId(any(), any()))
            .thenReturn(Optional.of(inventory));

        boolean reserved = inventoryService.reserveStock(UUID.randomUUID(), UUID.randomUUID(), 5);

        assertFalse(reserved);
    }

    @Test
    void shouldConfirmStockUsingFifoBatches() {
        UUID pharmacyId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();

        Inventory inventory = new Inventory();
        inventory.setId(UUID.randomUUID());
        inventory.setTotalQuantity(10);
        inventory.setReservedQuantity(5);

        Batch first = new Batch();
        first.setInventory(inventory);
        first.setStatus(BatchStatus.ACTIVE);
        first.setQuantity(3);
        first.setExpiryDate(LocalDate.now().plusDays(2));

        Batch second = new Batch();
        second.setInventory(inventory);
        second.setStatus(BatchStatus.ACTIVE);
        second.setQuantity(4);
        second.setExpiryDate(LocalDate.now().plusDays(10));

        when(inventoryRepository.findWithLockByPharmacyIdAndMedicineId(pharmacyId, medicineId))
            .thenReturn(Optional.of(inventory));
        when(batchRepository.findByInventoryIdAndStatusOrderByExpiryDateAsc(inventory.getId(), BatchStatus.ACTIVE))
            .thenReturn(List.of(first, second));

        boolean confirmed = inventoryService.confirmStock(pharmacyId, medicineId, 5, referenceId);

        assertTrue(confirmed);
        assertEquals(0, first.getQuantity());
        assertEquals(2, second.getQuantity());
        assertEquals(5, inventory.getTotalQuantity());
        assertEquals(0, inventory.getReservedQuantity());

        verify(batchRepository).saveAll(any());
        verify(inventoryRepository).save(inventory);
        verify(stockMovementRepository).save(any());
    }

    @Test
    void shouldFailReleaseWhenQuantityGreaterThanReserved() {
        Inventory inventory = new Inventory();
        inventory.setReservedQuantity(1);

        when(inventoryRepository.findWithLockByPharmacyIdAndMedicineId(any(), any()))
            .thenReturn(Optional.of(inventory));

        assertThrows(InventoryValidationException.class,
            () -> inventoryService.releaseStock(UUID.randomUUID(), UUID.randomUUID(), 2, UUID.randomUUID()));
    }
}