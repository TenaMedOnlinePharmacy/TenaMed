package com.TenaMed.inventory.mapper;

import com.TenaMed.inventory.dto.AddBatchRequest;
import com.TenaMed.inventory.dto.BatchResponse;
import com.TenaMed.inventory.entity.Batch;
import com.TenaMed.inventory.entity.Inventory;
import com.TenaMed.inventory.enums.BatchStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BatchMapperTests {

    private final BatchMapper mapper = new BatchMapper();

    @Test
    void shouldMapAddBatchRequestToEntity() {
        Inventory inventory = new Inventory();
        inventory.setId(UUID.randomUUID());

        AddBatchRequest request = new AddBatchRequest();
        request.setBatchNumber("B-1");
        request.setQuantity(50);
        request.setStatus(BatchStatus.ACTIVE);

        Batch batch = mapper.toEntity(request, inventory);

        assertEquals(inventory, batch.getInventory());
        assertEquals("B-1", batch.getBatchNumber());
        assertEquals(50, batch.getQuantity());
    }

    @Test
    void shouldMapEntityToResponse() {
        Inventory inventory = new Inventory();
        inventory.setId(UUID.randomUUID());

        Batch batch = new Batch();
        batch.setId(UUID.randomUUID());
        batch.setInventory(inventory);
        batch.setBatchNumber("B-RES");
        batch.setQuantity(70);
        batch.setStatus(BatchStatus.ACTIVE);

        BatchResponse response = mapper.toResponse(batch);

        assertEquals(batch.getId(), response.getId());
        assertEquals(inventory.getId(), response.getInventoryId());
        assertEquals("B-RES", response.getBatchNumber());
    }
}