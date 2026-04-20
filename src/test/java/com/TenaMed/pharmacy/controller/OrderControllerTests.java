package com.TenaMed.pharmacy.controller;

import com.TenaMed.pharmacy.dto.request.CreateOrderRequest;
import com.TenaMed.pharmacy.dto.request.RejectOrderRequest;
import com.TenaMed.pharmacy.dto.request.UpdatePaymentStatusRequest;
import com.TenaMed.pharmacy.dto.response.OrderResponse;
import com.TenaMed.pharmacy.enums.OrderStatus;
import com.TenaMed.pharmacy.enums.PaymentStatus;
import com.TenaMed.pharmacy.enums.StaffRole;
import com.TenaMed.pharmacy.service.OrderService;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void shouldCreateOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setPharmacyId(UUID.randomUUID());
        request.setPrescriptionItemIds(List.of(UUID.randomUUID()));

        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "user@test.com",
            "pwd",
            List.of(),
            true
        );

        OrderResponse response = OrderResponse.builder().id(UUID.randomUUID()).status(OrderStatus.PENDING_REVIEW).build();
        when(orderService.createOrder(any(CreateOrderRequest.class), any(UUID.class))).thenReturn(response);

        mockMvc.perform(post("/api/orders")
            .principal(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
    }

    @Test
    void shouldRejectCreateOrderWhenUnauthenticated() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setPharmacyId(UUID.randomUUID());
        request.setPrescriptionItemIds(List.of(UUID.randomUUID()));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void shouldAcceptOrder() throws Exception {
        UUID id = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
            actorUserId,
            UUID.randomUUID(),
            "owner@test.com",
            "pwd",
            List.of(new SimpleGrantedAuthority("ROLE_OWNER")),
            true
        );
        OrderResponse response = OrderResponse.builder().status(OrderStatus.PENDING_PAYMENT).build();
        when(orderService.acceptOrder(eq(id), eq(actorUserId), eq(StaffRole.OWNER))).thenReturn(response);

        mockMvc.perform(post("/api/orders/{id}/accept", id)
                .principal(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    void shouldReturnForbiddenForAcceptOrderWhenRoleNotAllowed() throws Exception {
        UUID id = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
            actorUserId,
            UUID.randomUUID(),
            "patient@test.com",
            "pwd",
            List.of(new SimpleGrantedAuthority("ROLE_PATIENT")),
            true
        );

        mockMvc.perform(post("/api/orders/{id}/accept", id)
                .principal(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Only pharmacy owner or pharmacist can accept orders"));
    }

    @Test
    void shouldRejectOrder() throws Exception {
        UUID id = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        RejectOrderRequest request = new RejectOrderRequest();
        request.setRejectionReason("Out of stock");
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
            actorUserId,
            UUID.randomUUID(),
            "owner@test.com",
            "pwd",
            List.of(new SimpleGrantedAuthority("ROLE_OWNER")),
            true
        );
        OrderResponse response = OrderResponse.builder().status(OrderStatus.REJECTED).rejectionReason("Out of stock").build();
        when(orderService.rejectOrder(eq(id), eq("Out of stock"), eq(actorUserId), eq(StaffRole.OWNER))).thenReturn(response);

        mockMvc.perform(post("/api/orders/{id}/reject", id)
                .principal(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void shouldUpdatePaymentStatus() throws Exception {
        UUID id = UUID.randomUUID();
        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest();
        request.setPaymentStatus(PaymentStatus.SUCCESS);
        OrderResponse response = OrderResponse.builder().status(OrderStatus.CONFIRMED).paymentStatus(PaymentStatus.SUCCESS).build();
        when(orderService.updatePaymentStatus(id, PaymentStatus.SUCCESS)).thenReturn(response);

        mockMvc.perform(post("/api/orders/{id}/payment-status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}