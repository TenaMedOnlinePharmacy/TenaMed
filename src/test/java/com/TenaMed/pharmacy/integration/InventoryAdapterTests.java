package com.TenaMed.pharmacy.integration;

import com.TenaMed.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryAdapterTests {

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryAdapter inventoryAdapter;

    @Test
    void shouldDelegateAvailabilityCheck() {
        UUID pharmacyId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        when(inventoryService.checkAvailability(pharmacyId, medicineId, 5)).thenReturn(true);

        boolean actual = inventoryAdapter.checkAvailability(pharmacyId, medicineId, 5);

        assertTrue(actual);
        verify(inventoryService).checkAvailability(pharmacyId, medicineId, 5);
    }

    @Test
    void shouldDelegateReserveAndRelease() {
        UUID pharmacyId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        when(inventoryService.reserveStock(pharmacyId, medicineId, 2)).thenReturn(true);

        assertTrue(inventoryAdapter.reserveStock(pharmacyId, medicineId, 2));
        inventoryAdapter.releaseStock(pharmacyId, medicineId, 2);

        verify(inventoryService).reserveStock(pharmacyId, medicineId, 2);
        verify(inventoryService).releaseStock(pharmacyId, medicineId, 2);
    }
}