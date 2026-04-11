package com.TenaMed.pharmacy.integration;

import com.TenaMed.pharmacy.entity.Order;
import com.TenaMed.pharmacy.enums.OrderStatus;
import com.TenaMed.pharmacy.enums.PaymentStatus;
import com.TenaMed.inventory.service.InventoryService;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class PharmacyLifecycleIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.TenaMed.pharmacy.repository.OrderRepository orderRepository;

    @MockitoBean
    private InventoryService inventoryService;

    @AfterEach
    void clearSecurityContext() {
      SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCompleteFullOrderLifecycle() throws Exception {
        when(inventoryService.checkAvailability(any(), any(), any())).thenReturn(true);
        when(inventoryService.reserveStock(any(), any(), any())).thenReturn(true);

        UUID ownerId = UUID.randomUUID();
        UUID staffUserId = UUID.randomUUID();

        String createPharmacyBody = """
            {
              "name": "Lifecycle Pharmacy",
              "legalName": "Lifecycle Pharmacy PLC",
              "licenseNumber": "LIC-LIFE-100",
              "email": "life@pharmacy.com",
              "phone": "+251900111111",
              "ownerId": "%s"
            }
            """.formatted(ownerId);

        MvcResult createPharmacyResult = mockMvc.perform(post("/api/pharmacies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPharmacyBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Lifecycle Pharmacy"))
            .andReturn();

        UUID pharmacyId = readUuid(createPharmacyResult, "id");

        setAdminPrincipal(ownerId);

        mockMvc.perform(post("/api/pharmacies/{id}/verify", pharmacyId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("VERIFIED"));

        String addStaffBody = """
            {
              "userId": "%s",
              "staffRole": "PHARMACIST",
              "employmentStatus": "ACTIVE",
              "canVerifyPrescriptions": true,
              "canManageInventory": true
            }
            """.formatted(staffUserId);

        mockMvc.perform(post("/api/pharmacies/{id}/staff", pharmacyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addStaffBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.staffRole").value("PHARMACIST"));

        UUID medicineId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        String createOrderBody = """
            {
              "customerId": "%s",
              "pharmacyId": "%s",
              "items": [
                {
                  "inventoryId": "%s",
                  "medicineId": "%s",
                  "quantity": 2,
                  "unitPrice": 45.00
                }
              ]
            }
            """.formatted(UUID.randomUUID(), pharmacyId, inventoryId, medicineId);

        MvcResult createOrderResult = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createOrderBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
            .andReturn();

        UUID orderId = readUuid(createOrderResult, "id");

        String acceptOrderBody = """
            {
              "actorUserId": "%s",
              "actorRole": "PHARMACIST"
            }
            """.formatted(staffUserId);

        mockMvc.perform(post("/api/orders/{id}/accept", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(acceptOrderBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        String paymentCallbackBody = """
            {
              "paymentStatus": "SUCCESS"
            }
            """;

        mockMvc.perform(post("/api/orders/{id}/payment-status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentCallbackBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));

        Optional<Order> savedOrder = orderRepository.findById(orderId);
        assertTrue(savedOrder.isPresent());
        assertEquals(OrderStatus.CONFIRMED, savedOrder.get().getStatus());
        assertEquals(PaymentStatus.SUCCESS, savedOrder.get().getPaymentStatus());
    }

    @Test
    void shouldReturnBadRequestWhenStockUnavailable() throws Exception {
        when(inventoryService.checkAvailability(any(), any(), any())).thenReturn(false);

        UUID ownerId = UUID.randomUUID();
        String createPharmacyBody = """
            {
              "name": "Edge Pharmacy",
              "legalName": "Edge Pharmacy PLC",
              "licenseNumber": "LIC-EDGE-101",
              "email": "edge@pharmacy.com",
              "phone": "+251900222222",
              "ownerId": "%s"
            }
            """.formatted(ownerId);

        MvcResult createPharmacyResult = mockMvc.perform(post("/api/pharmacies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPharmacyBody))
            .andExpect(status().isCreated())
            .andReturn();

        UUID pharmacyId = readUuid(createPharmacyResult, "id");

        setAdminPrincipal(ownerId);

        mockMvc.perform(post("/api/pharmacies/{id}/verify", pharmacyId))
            .andExpect(status().isOk());

        String createOrderBody = """
            {
              "customerId": "%s",
              "pharmacyId": "%s",
              "items": [
                {
                  "inventoryId": "%s",
                  "medicineId": "%s",
                  "quantity": 1,
                  "unitPrice": 20.00
                }
              ]
            }
            """.formatted(UUID.randomUUID(), pharmacyId, UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createOrderBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldRejectAcceptOrderForTechnicianRole() throws Exception {
        when(inventoryService.checkAvailability(any(), any(), any())).thenReturn(true);

        UUID ownerId = UUID.randomUUID();
        String createPharmacyBody = """
            {
              "name": "Role Pharmacy",
              "legalName": "Role Pharmacy PLC",
              "licenseNumber": "LIC-ROLE-102",
              "email": "role@pharmacy.com",
              "phone": "+251900333333",
              "ownerId": "%s"
            }
            """.formatted(ownerId);

        MvcResult createPharmacyResult = mockMvc.perform(post("/api/pharmacies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPharmacyBody))
            .andExpect(status().isCreated())
            .andReturn();

        UUID pharmacyId = readUuid(createPharmacyResult, "id");

        setAdminPrincipal(ownerId);

        mockMvc.perform(post("/api/pharmacies/{id}/verify", pharmacyId))
            .andExpect(status().isOk());

        String createOrderBody = """
            {
              "customerId": "%s",
              "pharmacyId": "%s",
              "items": [
                {
                  "inventoryId": "%s",
                  "medicineId": "%s",
                  "quantity": 1,
                  "unitPrice": 10.00
                }
              ]
            }
            """.formatted(UUID.randomUUID(), pharmacyId, UUID.randomUUID(), UUID.randomUUID());

        MvcResult createOrderResult = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createOrderBody))
            .andExpect(status().isCreated())
            .andReturn();

        UUID orderId = readUuid(createOrderResult, "id");

        String acceptOrderBody = """
            {
              "actorUserId": "%s",
              "actorRole": "TECHNICIAN"
            }
            """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/orders/{id}/accept", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(acceptOrderBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }

    private UUID readUuid(MvcResult result, String field) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(root.path(field).asText());
    }

    private void setAdminPrincipal(UUID userId) {
      AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
        userId,
        UUID.randomUUID(),
        "admin@tenamed.com",
        "pwd",
        java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
        true
      );
      SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
      );
    }
}