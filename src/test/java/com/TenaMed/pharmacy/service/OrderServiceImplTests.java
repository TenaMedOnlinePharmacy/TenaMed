package com.TenaMed.pharmacy.service;

import com.TenaMed.Normalization.entity.PrescriptionItem;
import com.TenaMed.Normalization.repository.PrescriptionItemRepository;
import com.TenaMed.inventory.entity.Batch;
import com.TenaMed.inventory.enums.BatchStatus;
import com.TenaMed.inventory.entity.Inventory;
import com.TenaMed.inventory.repository.BatchRepository;
import com.TenaMed.inventory.repository.InventoryRepository;
import com.TenaMed.pharmacy.dto.request.CreateOrderRequest;
import com.TenaMed.pharmacy.dto.response.OrderResponse;
import com.TenaMed.pharmacy.entity.Order;
import com.TenaMed.pharmacy.entity.OrderItem;
import com.TenaMed.medicine.entity.Medicine;
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
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    private PrescriptionItemRepository prescriptionItemRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void shouldCreateOrderOnHappyPath() {
        UUID pharmacyId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CreateOrderRequest request = buildCreateOrderRequest(pharmacyId);
        UUID prescriptionItemId = request.getPrescriptionItemIds().getFirst();
        UUID medicineId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        BigDecimal sellingPrice = new BigDecimal("75.50");

        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setId(pharmacyId);
        pharmacy.setStatus(PharmacyStatus.VERIFIED);

        Medicine medicine = mock(Medicine.class);
        when(medicine.getId()).thenReturn(medicineId);

        PrescriptionItem prescriptionItem = new PrescriptionItem();
        prescriptionItem.setId(prescriptionItemId);
        prescriptionItem.setMedicine(medicine);
        prescriptionItem.setQuantity(2);

        Inventory inventory = new Inventory();
        inventory.setId(inventoryId);

        Batch batch = new Batch();
        batch.setStatus(BatchStatus.ACTIVE);
        batch.setSellingPrice(sellingPrice);

        Order mappedOrder = new Order();
        mappedOrder.setId(UUID.randomUUID());
        mappedOrder.setPharmacy(pharmacy);
        mappedOrder.setStatus(OrderStatus.PENDING_REVIEW);
        mappedOrder.setPaymentStatus(PaymentStatus.PENDING);

        OrderResponse response = OrderResponse.builder().id(mappedOrder.getId()).status(OrderStatus.PENDING_REVIEW).build();

        when(pharmacyRepository.findById(pharmacyId)).thenReturn(Optional.of(pharmacy));
        when(prescriptionItemRepository.findAllById(request.getPrescriptionItemIds())).thenReturn(List.of(prescriptionItem));
        when(inventoryRepository.findByPharmacyIdAndMedicineId(pharmacyId, medicineId)).thenReturn(Optional.of(inventory));
        when(batchRepository.findByInventoryIdAndStatusOrderByExpiryDateAsc(inventoryId, BatchStatus.ACTIVE))
            .thenReturn(List.of(batch));
        when(inventoryService.checkAvailability(pharmacyId, medicineId, 2)).thenReturn(true);
        when(orderMapper.toEntity(request, pharmacy, customerId)).thenReturn(mappedOrder);
        when(orderRepository.save(mappedOrder)).thenReturn(mappedOrder);
        when(orderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(mappedOrder)).thenReturn(response);

        OrderResponse actual = orderService.createOrder(request, customerId);

        ArgumentCaptor<List<OrderItem>> orderItemsCaptor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(orderItemRepository).saveAll(orderItemsCaptor.capture());
        List<OrderItem> savedItems = orderItemsCaptor.getValue();

        assertEquals(mappedOrder.getId(), actual.getId());
        assertEquals(OrderStatus.PENDING_REVIEW, actual.getStatus());
        assertEquals(sellingPrice, savedItems.getFirst().getUnitPrice());
        assertEquals(new BigDecimal("151.00"), mappedOrder.getTotalAmount());
    }

    @Test
    void shouldFailCreateOrderWhenPharmacyNotVerified() {
        UUID pharmacyId = UUID.randomUUID();
        CreateOrderRequest request = buildCreateOrderRequest(pharmacyId);

        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setId(pharmacyId);
        pharmacy.setStatus(PharmacyStatus.PENDING);

        when(pharmacyRepository.findById(pharmacyId)).thenReturn(Optional.of(pharmacy));

        assertThrows(PharmacyValidationException.class, () -> orderService.createOrder(request, UUID.randomUUID()));
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
        request.setPharmacyId(pharmacyId);
        request.setPrescriptionItemIds(List.of(UUID.randomUUID()));
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