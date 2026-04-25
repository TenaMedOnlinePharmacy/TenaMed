package com.TenaMed.pharmacy.service.impl;

import com.TenaMed.Normalization.entity.PrescriptionItem;
import com.TenaMed.Normalization.repository.PrescriptionItemRepository;
import com.TenaMed.inventory.entity.Batch;
import com.TenaMed.inventory.enums.BatchStatus;
import com.TenaMed.inventory.repository.BatchRepository;
import com.TenaMed.inventory.repository.InventoryRepository;
import com.TenaMed.pharmacy.dto.request.CreateOrderFromCartRequest;
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
import com.TenaMed.medicine.repository.MedicineRepository;
import com.TenaMed.pharmacy.dto.response.PharmacyOrderResponse;
import com.TenaMed.pharmacy.entity.UserPharmacy;
import com.TenaMed.pharmacy.repository.UserPharmacyRepository;
import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.entity.PrescriptionType;
import com.TenaMed.prescription.service.PrescriptionService;
import com.TenaMed.events.DomainEventService;
import com.TenaMed.medicine.entity.Medicine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final DomainEventService domainEventService;
    private final UserPharmacyRepository userPharmacyRepository;
    private final MedicineRepository medicineRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            PharmacyRepository pharmacyRepository,
                            OrderMapper orderMapper,
                            InventoryService inventoryService,
                            PrescriptionService prescriptionService,
                            PrescriptionItemRepository prescriptionItemRepository,
                            InventoryRepository inventoryRepository,
                            BatchRepository batchRepository,
                            DomainEventService domainEventService,
                            UserPharmacyRepository userPharmacyRepository,
                            MedicineRepository medicineRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.orderMapper = orderMapper;
        this.inventoryService = inventoryService;
        this.prescriptionService = prescriptionService;
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.inventoryRepository = inventoryRepository;
        this.batchRepository = batchRepository;
        this.domainEventService = domainEventService;
        this.userPharmacyRepository = userPharmacyRepository;
        this.medicineRepository = medicineRepository;
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

        domainEventService.publish(
            "ORDER_CREATED",
            "ORDER",
            savedOrder.getId(),
            "USER",
            customerId,
            "PHARMACY",
            pharmacy.getId(),
            Map.of("status", savedOrder.getStatus().name())
        );

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    public OrderResponse acceptOrder(UUID orderId, UUID actorUserId, StaffRole actorRole) {
        Order order = fetchOrder(orderId);
        String oldStatus = String.valueOf(order.getStatus());

        if (actorRole != StaffRole.OWNER && actorRole != StaffRole.PHARMACIST) {
            throw new OrderAuthorizationException();
        }

        // Verify that the user belongs to the pharmacy of the order
        java.util.List<UserPharmacy> userPharmacies = userPharmacyRepository.findByUserId(actorUserId);
        boolean isAuthorized = userPharmacies.stream()
                .anyMatch(up -> up.getPharmacy().getId().equals(order.getPharmacy().getId()));

        if (!isAuthorized) {
            throw new OrderAuthorizationException("You are not authorized to accept orders for this pharmacy");
        }

        reserveOrderItems(order);

        order.setAcceptedBy(actorUserId);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        Order saved = orderRepository.save(order);
        domainEventService.publish(
            "ORDER_ACCEPTED",
            "ORDER",
            saved.getId(),
            actorRole == StaffRole.OWNER ? "PHARMACY_OWNER" : "PHARMACIST",
            actorUserId,
            "PHARMACY",
            saved.getPharmacy().getId(),
                Map.of("changes", Map.of("status", Map.of("old", oldStatus, "new", saved.getStatus().name())))
        );
        return orderMapper.toResponse(saved);
    }

    @Override
    public OrderResponse rejectOrder(UUID orderId, String rejectionReason, UUID actorUserId, StaffRole actorRole) {
        Order order = fetchOrder(orderId);
        String oldStatus = String.valueOf(order.getStatus());
        if (actorRole != StaffRole.OWNER && actorRole != StaffRole.PHARMACIST) {
            throw new OrderAuthorizationException();
        }

        // Verify that the user belongs to the pharmacy of the order
        java.util.List<UserPharmacy> userPharmacies = userPharmacyRepository.findByUserId(actorUserId);
        boolean isAuthorized = userPharmacies.stream()
                .anyMatch(up -> up.getPharmacy().getId().equals(order.getPharmacy().getId()));

        if (!isAuthorized) {
            throw new OrderAuthorizationException("You are not authorized to reject orders for this pharmacy");
        }

        order.setStatus(OrderStatus.REJECTED);
        order.setRejectionReason(rejectionReason);
        Order saved = orderRepository.save(order);
        domainEventService.publish(
            "ORDER_REJECTED",
            "ORDER",
            saved.getId(),
            actorRole == StaffRole.OWNER ? "PHARMACY_OWNER" : "PHARMACIST",
            actorUserId,
            "PHARMACY",
            saved.getPharmacy().getId(),
                Map.of("changes", Map.of("status", Map.of("old", oldStatus, "new", saved.getStatus().name())))
        );
        return orderMapper.toResponse(saved);
    }

    @Override
    public OrderResponse updatePaymentStatus(UUID orderId, PaymentStatus paymentStatus) {
        Order order = fetchOrder(orderId);
        String oldStatus = String.valueOf(order.getStatus());
        String oldPaymentStatus = String.valueOf(order.getPaymentStatus());
        order.setPaymentStatus(paymentStatus);
        if (paymentStatus == PaymentStatus.SUCCESS) {
            order.setStatus(OrderStatus.CONFIRMED);
        }
        if (paymentStatus == PaymentStatus.FAILED) {
            order.setStatus(OrderStatus.CANCELLED);
        }
        Order saved = orderRepository.save(order);
        domainEventService.publish(
            "ORDER_PAYMENT_UPDATED",
            "ORDER",
            saved.getId(),
            "SYSTEM",
            null,
            "PHARMACY",
            saved.getPharmacy().getId(),
            Map.of(
                "changes",
                Map.of(
                    "paymentStatus", Map.of("old", oldPaymentStatus, "new", String.valueOf(saved.getPaymentStatus())),
                    "status", Map.of("old", oldStatus, "new", String.valueOf(saved.getStatus()))
                )
            )
        );
        return orderMapper.toResponse(saved);
    }

    @Override
    public OrderResponse createOrderFromCart(UUID customerId, CreateOrderFromCartRequest request) {
        if (customerId == null) {
            throw new PharmacyValidationException("customerId is required");
        }
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new PharmacyValidationException("Cart items are required");
        }
        if (request.getPharmacyId() == null) {
            throw new PharmacyValidationException("pharmacyId is required");
        }

        UUID selectedPharmacyId = request.getPharmacyId();
        Pharmacy pharmacy = pharmacyRepository.findByIdAndStatus(request.getPharmacyId(), PharmacyStatus.VERIFIED)
                .orElseThrow(() -> new PharmacyValidationException("No verified pharmacy found for checkout"));

        Order order = new Order();
        order.setCustomerId(customerId);
        order.setPharmacy(pharmacy);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(PaymentStatus.PENDING);

        BigDecimal total = BigDecimal.ZERO;
        Set<OrderItem> orderItems = new LinkedHashSet<>();

        for (CreateOrderFromCartRequest.Item itemRequest : request.getItems()) {
            UUID medicineId = itemRequest.getMedicineId();
            Integer quantity = itemRequest.getQuantity();

            if (!inventoryService.checkAvailability(selectedPharmacyId, medicineId, quantity)) {
                throw new PharmacyValidationException("Insufficient stock for medicine " + medicineId);
            }

            UUID inventoryId = inventoryRepository.findByPharmacyIdAndMedicineId(selectedPharmacyId, medicineId)
                    .map(inv -> inv.getId())
                    .orElseThrow(() -> new PharmacyValidationException("Inventory not found for medicine " + medicineId));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setInventoryId(inventoryId);
            orderItem.setMedicineId(medicineId);
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(itemRequest.getUnitPrice());
            orderItems.add(orderItem);

            total = total.add(itemRequest.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
        }

        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);

        orderItems.forEach(item -> item.setOrder(savedOrder));
        List<OrderItem> savedItems = orderItemRepository.saveAll(orderItems.stream().toList());
        savedOrder.getItems().addAll(savedItems);

        for (OrderItem orderItem : savedItems) {
            boolean reserved = inventoryService.reserveStock(selectedPharmacyId, orderItem.getMedicineId(), orderItem.getQuantity(), savedOrder.getId());
            if (!reserved) {
                throw new PharmacyValidationException("Unable to reserve stock for medicine " + orderItem.getMedicineId());
            }
        }

        domainEventService.publish(
                "ORDER_CREATED",
                "ORDER",
                savedOrder.getId(),
                "USER",
                customerId,
                "PHARMACY",
                selectedPharmacyId,
                Map.of("source", "CART", "status", savedOrder.getStatus().name())
        );

        return orderMapper.toResponse(savedOrder);
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

    @Override
    public java.util.List<PharmacyOrderResponse> getPharmacyOrders(UUID ownerId) {
        java.util.List<UserPharmacy> userPharmacies = userPharmacyRepository.findByUserId(ownerId);
        if (userPharmacies.isEmpty()) {
            throw new PharmacyNotFoundException("No pharmacy found for the given owner");
        }

        java.util.List<UUID> pharmacyIds = userPharmacies.stream()
                .map(up -> up.getPharmacy().getId())
                .toList();

        java.util.List<Order> orders = pharmacyIds.stream()
                .flatMap(pid -> orderRepository.findByPharmacyId(pid).stream())
                .toList();

        return orders.stream().map(this::mapToPharmacyOrderResponse).toList();
    }

    private PharmacyOrderResponse mapToPharmacyOrderResponse(Order order) {
        Prescription prescription = null;
        if (order.getPrescriptionId() != null) {
            prescription = prescriptionService.getPrescription(order.getPrescriptionId());
        }

        java.util.List<PharmacyOrderResponse.PharmacyOrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> {
                    Medicine medicine = medicineRepository.findById(item.getMedicineId()).orElse(null);
                    return PharmacyOrderResponse.PharmacyOrderItemResponse.builder()
                            .medicineName(medicine != null ? medicine.getName() : "Unknown Medicine")
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .build();
                }).toList();

        String prescriptionImage = (prescription != null) ? prescription.getOriginalImages() : null;

        return PharmacyOrderResponse.builder()
                .orderId(order.getId())
                .prescriptionImage(prescriptionImage)
                .orderItems(itemResponses)
                .type(prescription != null ? prescription.getType() : null)
                .highRisk(prescription != null ? prescription.getHighRisk() : null)
                .confidenceScore(prescription != null ? prescription.getConfidenceScore() : null)
                .totalAmount(order.getTotalAmount())
                .build();
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