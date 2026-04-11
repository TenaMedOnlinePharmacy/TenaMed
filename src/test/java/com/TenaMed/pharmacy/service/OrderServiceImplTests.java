package com.TenaMed.pharmacy.service;

import com.TenaMed.pharmacy.dto.request.CreateOrderRequest;
import com.TenaMed.pharmacy.dto.request.OrderItemRequest;
import com.TenaMed.pharmacy.dto.response.OrderResponse;
import com.TenaMed.pharmacy.entity.Order;
import com.TenaMed.pharmacy.entity.OrderItem;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.enums.OrderStatus;
import com.TenaMed.pharmacy.enums.PaymentStatus;
import com.TenaMed.pharmacy.enums.PharmacyStatus;
import com.TenaMed.pharmacy.enums.StaffRole;
import com.TenaMed.pharmacy.exception.OrderAuthorizationException;
import com.TenaMed.pharmacy.exception.PharmacyValidationException;
import com.TenaMed.inventory.service.InventoryService;
import com.TenaMed.pharmacy.mapper.OrderMapper;
import com.TenaMed.pharmacy.repository.OrderItemRepository;
import com.TenaMed.pharmacy.repository.OrderRepository;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
import com.TenaMed.pharmacy.service.impl.OrderServiceImpl;
import com.TenaMed.prescription.service.PrescriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTests {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private PharmacyRepository pharmacyRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private PrescriptionService prescriptionService;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void shouldCreateOrderOnHappyPath() {
        UUID pharmacyId = UUID.randomUUID();
        CreateOrderRequest request = buildCreateOrderRequest(pharmacyId);

        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setId(pharmacyId);
        pharmacy.setStatus(PharmacyStatus.VERIFIED);

        Order mappedOrder = new Order();
        mappedOrder.setId(UUID.randomUUID());
        mappedOrder.setPharmacy(pharmacy);
        mappedOrder.setStatus(OrderStatus.PENDING_REVIEW);
        mappedOrder.setPaymentStatus(PaymentStatus.PENDING);

        OrderItem item = new OrderItem();
        item.setOrder(mappedOrder);
        item.setInventoryId(request.getItems().getFirst().getInventoryId());
        item.setMedicineId(request.getItems().getFirst().getMedicineId());
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("50.00"));

        OrderResponse response = OrderResponse.builder().id(mappedOrder.getId()).status(OrderStatus.PENDING_REVIEW).build();

        when(pharmacyRepository.findById(pharmacyId)).thenReturn(Optional.of(pharmacy));
        when(inventoryService.checkAvailability(pharmacyId, request.getItems().getFirst().getMedicineId(), 2)).thenReturn(true);
        when(orderMapper.toEntity(request, pharmacy)).thenReturn(mappedOrder);
        when(orderRepository.save(mappedOrder)).thenReturn(mappedOrder);
        when(orderMapper.toOrderItemEntity(any(OrderItemRequest.class), any(Order.class))).thenReturn(item);
        when(orderItemRepository.saveAll(any())).thenReturn(List.of(item));
        when(orderMapper.toResponse(mappedOrder)).thenReturn(response);

        OrderResponse actual = orderService.createOrder(request);

        assertEquals(mappedOrder.getId(), actual.getId());
        assertEquals(OrderStatus.PENDING_REVIEW, actual.getStatus());
    }

    @Test
    void shouldFailCreateOrderWhenPharmacyNotVerified() {
        UUID pharmacyId = UUID.randomUUID();
        CreateOrderRequest request = buildCreateOrderRequest(pharmacyId);

        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setId(pharmacyId);
        pharmacy.setStatus(PharmacyStatus.PENDING);

        when(pharmacyRepository.findById(pharmacyId)).thenReturn(Optional.of(pharmacy));

        assertThrows(PharmacyValidationException.class, () -> orderService.createOrder(request));
    }

    @Test
    void shouldAcceptOrderAndMoveToPendingPayment() {
        UUID orderId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PENDING_REVIEW);
        order.setItems(Set.of());
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setId(UUID.randomUUID());
        order.setPharmacy(pharmacy);

        OrderResponse response = OrderResponse.builder().status(OrderStatus.PENDING_PAYMENT).acceptedBy(actorId).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(inventoryService.reserveStock(order.getPharmacy().getId(), itemMedicineId(order), 1)).thenReturn(true);
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(response);

        OrderResponse actual = orderService.acceptOrder(orderId, actorId, StaffRole.PHARMACIST);

        assertEquals(OrderStatus.PENDING_PAYMENT, actual.getStatus());
        assertEquals(actorId, actual.getAcceptedBy());
    }

    @Test
    void shouldRejectAcceptOrderWhenRoleNotAllowed() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setId(UUID.randomUUID());
        order.setPharmacy(pharmacy);
        order.setItems(Set.of());

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(OrderAuthorizationException.class,
            () -> orderService.acceptOrder(orderId, UUID.randomUUID(), StaffRole.TECHNICIAN));
    }

    @Test
    void shouldUpdatePaymentSuccessToConfirmed() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setId(UUID.randomUUID());
        order.setPharmacy(pharmacy);
        order.setItems(Set.of());

        OrderResponse response = OrderResponse.builder().status(OrderStatus.CONFIRMED).paymentStatus(PaymentStatus.SUCCESS).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(response);

        OrderResponse actual = orderService.updatePaymentStatus(orderId, PaymentStatus.SUCCESS);

        assertEquals(OrderStatus.CONFIRMED, actual.getStatus());
        assertEquals(PaymentStatus.SUCCESS, actual.getPaymentStatus());
    }

    private CreateOrderRequest buildCreateOrderRequest(UUID pharmacyId) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(UUID.randomUUID());
        request.setPharmacyId(pharmacyId);

        OrderItemRequest item = new OrderItemRequest();
        item.setInventoryId(UUID.randomUUID());
        item.setMedicineId(UUID.randomUUID());
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("50.00"));
        request.setItems(List.of(item));
        return request;
    }

    private UUID itemMedicineId(Order order) {
        if (order.getItems().isEmpty()) {
            OrderItem item = new OrderItem();
            item.setMedicineId(UUID.randomUUID());
            item.setQuantity(1);
            order.setItems(Set.of(item));
        }
        return order.getItems().iterator().next().getMedicineId();
    }
}