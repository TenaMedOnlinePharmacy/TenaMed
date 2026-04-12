package com.TenaMed.pharmacy.mapper;

import com.TenaMed.pharmacy.dto.request.CreateOrderRequest;
import com.TenaMed.pharmacy.dto.response.OrderResponse;
import com.TenaMed.pharmacy.entity.Order;
import com.TenaMed.pharmacy.entity.OrderItem;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.enums.OrderStatus;
import com.TenaMed.pharmacy.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderMapperTests {

    private final OrderMapper orderMapper = new OrderMapper();

    @Test
    void shouldMapCreateOrderRequestToOrderEntity() {
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setId(UUID.randomUUID());

        CreateOrderRequest request = new CreateOrderRequest();
        UUID customerId = UUID.randomUUID();
        request.setPharmacyId(pharmacy.getId());
        request.setPrescriptionItemIds(List.of(UUID.randomUUID()));

        Order order = orderMapper.toEntity(request, pharmacy, customerId);

        assertEquals(customerId, order.getCustomerId());
        assertEquals(OrderStatus.PENDING_REVIEW, order.getStatus());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
    }

    @Test
    void shouldMapOrderEntityToResponse() {
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setId(UUID.randomUUID());

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomerId(UUID.randomUUID());
        order.setPharmacy(pharmacy);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setTotalAmount(new BigDecimal("120.00"));

        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setOrder(order);
        item.setInventoryId(UUID.randomUUID());
        item.setMedicineId(UUID.randomUUID());
        item.setQuantity(3);
        item.setUnitPrice(new BigDecimal("40.00"));
        order.setItems(Set.of(item));

        OrderResponse response = orderMapper.toResponse(order);

        assertEquals(order.getId(), response.getId());
        assertEquals(1, response.getItems().size());
        assertEquals(new BigDecimal("120.00"), response.getTotalAmount());
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
    }
}