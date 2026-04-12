package com.TenaMed.pharmacy.service.impl;

import com.TenaMed.Normalization.entity.PrescriptionItem;
import com.TenaMed.Normalization.repository.PrescriptionItemRepository;
import com.TenaMed.inventory.entity.Batch;
import com.TenaMed.inventory.enums.BatchStatus;
import com.TenaMed.inventory.repository.BatchRepository;
import com.TenaMed.inventory.repository.InventoryRepository;
import com.TenaMed.pharmacy.dto.request.CreateOrderRequest;
import com.TenaMed.pharmacy.dto.response.OrderResponse;
import com.TenaMed.pharmacy.entity.Order;
import com.TenaMed.pharmacy.entity.OrderItem;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.enums.OrderStatus;
import com.TenaMed.pharmacy.enums.PaymentStatus;
import com.TenaMed.pharmacy.enums.PharmacyStatus;
import com.TenaMed.pharmacy.enums.StaffRole;
import com.TenaMed.pharmacy.exception.OrderAuthorizationException;
import com.TenaMed.pharmacy.exception.OrderNotFoundException;
import com.TenaMed.pharmacy.exception.PharmacyNotFoundException;
import com.TenaMed.pharmacy.exception.PharmacyValidationException;
import com.TenaMed.pharmacy.exception.PrescriptionValidationException;
import com.TenaMed.pharmacy.mapper.OrderMapper;
import com.TenaMed.pharmacy.repository.OrderItemRepository;
import com.TenaMed.pharmacy.repository.OrderRepository;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
import com.TenaMed.pharmacy.service.OrderService;
import com.TenaMed.inventory.service.InventoryService;
import com.TenaMed.prescription.service.PrescriptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PharmacyRepository pharmacyRepository;
    private final OrderMapper orderMapper;
    private final InventoryService inventoryService;
    private final PrescriptionService prescriptionService;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final InventoryRepository inventoryRepository;
    private final BatchRepository batchRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            PharmacyRepository pharmacyRepository,
                            OrderMapper orderMapper,
                            InventoryService inventoryService,
                            PrescriptionService prescriptionService,
                            PrescriptionItemRepository prescriptionItemRepository,
                            InventoryRepository inventoryRepository,
                            BatchRepository batchRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.orderMapper = orderMapper;
        this.inventoryService = inventoryService;
        this.prescriptionService = prescriptionService;
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.inventoryRepository = inventoryRepository;
        this.batchRepository = batchRepository;
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request, UUID customerId) {
        Pharmacy pharmacy = pharmacyRepository.findById(request.getPharmacyId())
            .orElseThrow(() -> new PharmacyNotFoundException(request.getPharmacyId()));

        if (pharmacy.getStatus() != PharmacyStatus.VERIFIED) {
            throw new PharmacyValidationException("Pharmacy must be VERIFIED before placing orders");
        }

        if (request.getPrescriptionId() != null) {
            if (prescriptionService.getPrescription(request.getPrescriptionId()) == null) {
                throw new PrescriptionValidationException(request.getPrescriptionId());
            }
        }

        List<OrderItem> derivedItems = buildOrderItems(request);
        validateItemAvailability(request.getPharmacyId(), derivedItems);

        Order order = orderMapper.toEntity(request, pharmacy, customerId);
        order.setStatus(OrderStatus.PENDING_REVIEW);
        order.setTotalAmount(calculateTotal(derivedItems));
        Order savedOrder = orderRepository.save(order);

        derivedItems.forEach(item -> item.setOrder(savedOrder));
        List<OrderItem> savedItems = orderItemRepository.saveAll(derivedItems);
        savedOrder.getItems().addAll(savedItems);

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    public OrderResponse acceptOrder(UUID orderId, UUID actorUserId, StaffRole actorRole) {
        Order order = fetchOrder(orderId);

        if (actorRole != StaffRole.OWNER && actorRole != StaffRole.PHARMACIST) {
            throw new OrderAuthorizationException();
        }

        reserveOrderItems(order);

        order.setAcceptedBy(actorUserId);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    public OrderResponse rejectOrder(UUID orderId, String rejectionReason) {
        Order order = fetchOrder(orderId);
        order.setStatus(OrderStatus.REJECTED);
        order.setRejectionReason(rejectionReason);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    public OrderResponse updatePaymentStatus(UUID orderId, PaymentStatus paymentStatus) {
        Order order = fetchOrder(orderId);
        order.setPaymentStatus(paymentStatus);
        if (paymentStatus == PaymentStatus.SUCCESS) {
            order.setStatus(OrderStatus.CONFIRMED);
        }
        if (paymentStatus == PaymentStatus.FAILED) {
            order.setStatus(OrderStatus.CANCELLED);
        }
        return orderMapper.toResponse(orderRepository.save(order));
    }

    private Order fetchOrder(UUID orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void validateItemAvailability(UUID pharmacyId, List<OrderItem> items) {
        for (OrderItem item : items) {
            boolean available = inventoryService.checkAvailability(pharmacyId, item.getMedicineId(), item.getQuantity());
            if (!available) {
                throw new PharmacyValidationException("Insufficient stock for medicine " + item.getMedicineId());
            }
        }
    }

    private List<OrderItem> buildOrderItems(CreateOrderRequest request) {
        List<UUID> prescriptionItemIds = request.getPrescriptionItemIds();
        List<PrescriptionItem> prescriptionItems = prescriptionItemRepository.findAllById(prescriptionItemIds);

        if (prescriptionItems.size() != prescriptionItemIds.size()) {
            throw new PharmacyValidationException("Some prescription item IDs do not exist");
        }

        Map<UUID, PrescriptionItem> byId = prescriptionItems.stream()
            .collect(Collectors.toMap(PrescriptionItem::getId, Function.identity()));

        return prescriptionItemIds.stream()
            .map(id -> toOrderItem(byId.get(id), request))
            .toList();
    }

    private OrderItem toOrderItem(PrescriptionItem prescriptionItem, CreateOrderRequest request) {
        if (prescriptionItem == null || prescriptionItem.getMedicine() == null || prescriptionItem.getMedicine().getId() == null) {
            throw new PharmacyValidationException("Invalid prescription item reference");
        }
        if (prescriptionItem.getQuantity() == null || prescriptionItem.getQuantity() <= 0) {
            throw new PharmacyValidationException("Prescription item quantity must be greater than zero");
        }
        if (request.getPrescriptionId() != null
            && !request.getPrescriptionId().equals(prescriptionItem.getPrescription().getId())) {
            throw new PharmacyValidationException("Prescription item does not belong to the provided prescriptionId");
        }

        UUID medicineId = prescriptionItem.getMedicine().getId();
        UUID inventoryId = inventoryRepository.findByPharmacyIdAndMedicineId(request.getPharmacyId(), medicineId)
            .map(inv -> inv.getId())
            .orElseThrow(() -> new PharmacyValidationException("Inventory not found for medicine " + medicineId));

        Batch activeBatch = batchRepository
            .findByInventoryIdAndStatusOrderByExpiryDateAsc(inventoryId, BatchStatus.ACTIVE)
            .stream()
            .findFirst()
            .orElseThrow(() -> new PharmacyValidationException("No active batch found for inventory " + inventoryId));

        if (activeBatch.getSellingPrice() == null) {
            throw new PharmacyValidationException("Active batch selling price is missing for inventory " + inventoryId);
        }

        OrderItem orderItem = new OrderItem();
        orderItem.setInventoryId(inventoryId);
        orderItem.setMedicineId(medicineId);
        orderItem.setQuantity(prescriptionItem.getQuantity());
        orderItem.setUnitPrice(activeBatch.getSellingPrice());
        return orderItem;
    }

    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void reserveOrderItems(Order order) {
        for (OrderItem item : order.getItems()) {
            boolean reserved = inventoryService.reserveStock(
                order.getPharmacy().getId(),
                item.getMedicineId(),
                item.getQuantity()
            );
            if (!reserved) {
                throw new PharmacyValidationException("Unable to reserve stock for medicine " + item.getMedicineId());
            }
        }
    }
}