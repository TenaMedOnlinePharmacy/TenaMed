package com.TenaMed.inventory.controller;

import com.TenaMed.inventory.dto.AddBatchRequest;
import com.TenaMed.inventory.dto.BatchResponse;
import com.TenaMed.inventory.dto.CreateInventoryRequest;
import com.TenaMed.inventory.dto.InventoryResponse;
import com.TenaMed.inventory.dto.StockActionRequest;
import com.TenaMed.inventory.exception.InventoryValidationException;
import com.TenaMed.inventory.service.InventoryService;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    void shouldCreateInventory() throws Exception {
        CreateInventoryRequest request = new CreateInventoryRequest();
        UUID pharmacyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        request.setPharmacyId(pharmacyId);
        request.setProductId(productId);
        request.setTotalQuantity(10);
        request.setReservedQuantity(0);

        InventoryResponse response = InventoryResponse.builder()
            .id(UUID.randomUUID())
            .totalQuantity(10)
            .reservedQuantity(0)
            .availableQuantity(10)
            .build();

        when(inventoryService.createInventory(any(CreateInventoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.availableQuantity").value(10));
    }

    @Test
    void shouldAddBatch() throws Exception {
        AddBatchRequest request = new AddBatchRequest();
        request.setBrandName("Aspirin Brand");
        request.setManufacturer("Pharma Corp");
        request.setQuantity(50);

        BatchResponse response = BatchResponse.builder()
            .id(UUID.randomUUID())
            .batchNumber("B-123")
            .quantity(50)
            .build();

        when(inventoryService.addBatch(any(AddBatchRequest.class), any(), any())).thenReturn(response);

        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "staff@test.com",
            "pwd",
            java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PHARMACIST")),
            true
        );

        org.springframework.mock.web.MockMultipartFile batchPart = new org.springframework.mock.web.MockMultipartFile(
            "batch", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/inventory/batch")
                .file(batchPart)
                .principal(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.quantity").value(50));
    }

    @Test
    void shouldCheckAvailability() throws Exception {
        UUID pharmacyId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(inventoryService.checkAvailability(pharmacyId, productId, 2)).thenReturn(true);

        mockMvc.perform(get("/api/inventory/check")
                .param("pharmacyId", pharmacyId.toString())
                .param("productId", productId.toString())
                .param("quantity", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void shouldReserveStock() throws Exception {
        StockActionRequest request = new StockActionRequest();
        request.setPharmacyId(UUID.randomUUID());
        request.setProductId(UUID.randomUUID());
        request.setQuantity(3);
        request.setReferenceId(UUID.randomUUID());

        when(inventoryService.reserveStock(
            eq(request.getPharmacyId()),
            eq(request.getProductId()),
            eq(3),
            eq(request.getReferenceId())
        )).thenReturn(true);

        mockMvc.perform(post("/api/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reserved").value(true));
    }

    @Test
    void shouldReturnBadRequestOnReleaseValidationFailure() throws Exception {
        StockActionRequest request = new StockActionRequest();
        request.setPharmacyId(UUID.randomUUID());
        request.setProductId(UUID.randomUUID());
        request.setQuantity(3);

        doThrow(new InventoryValidationException("Cannot release more than reserved quantity"))
            .when(inventoryService).releaseStock(any(), any(), any(), any());

        mockMvc.perform(post("/api/inventory/release")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Cannot release more than reserved quantity"));
    }
}