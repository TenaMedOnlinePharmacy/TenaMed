package com.TenaMed.inventory.mapper;

import com.TenaMed.inventory.dto.BatchResponse;
import com.TenaMed.inventory.dto.CreateInventoryRequest;
import com.TenaMed.inventory.dto.InventoryResponse;
import com.TenaMed.inventory.entity.Inventory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryMapperTests {

    private final InventoryMapper mapper = new InventoryMapper();

    @Test
    void shouldMapCreateRequestToEntity() {
        CreateInventoryRequest request = new CreateInventoryRequest();
        request.setPharmacyId(UUID.randomUUID());
        request.setMedicineId(UUID.randomUUID());
        request.setTotalQuantity(100);
        request.setReservedQuantity(10);
        request.setReorderLevel(20);

        Inventory inventory = mapper.toEntity(request);

        assertEquals(request.getPharmacyId(), inventory.getPharmacyId());
        assertEquals(request.getMedicineId(), inventory.getMedicineId());
        assertEquals(100, inventory.getTotalQuantity());
        assertEquals(10, inventory.getReservedQuantity());
    }

    @Test
    void shouldMapEntityToResponse() {
        Inventory inventory = new Inventory();
        inventory.setId(UUID.randomUUID());
        inventory.setPharmacyId(UUID.randomUUID());
        inventory.setMedicineId(UUID.randomUUID());
        inventory.setTotalQuantity(80);
        inventory.setReservedQuantity(5);
        inventory.setReorderLevel(10);

        InventoryResponse response = mapper.toResponse(inventory, List.of(BatchResponse.builder().build()));

        assertEquals(75, response.getAvailableQuantity());
        assertEquals(1, response.getBatches().size());
    }
}