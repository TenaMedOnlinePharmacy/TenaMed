package com.TenaMed.pharmacy.service.impl;

import com.TenaMed.Normalization.entity.PrescriptionItem;
import com.TenaMed.Normalization.repository.PrescriptionItemRepository;
import com.TenaMed.inventory.entity.Batch;
import com.TenaMed.inventory.enums.BatchStatus;
import com.TenaMed.inventory.repository.BatchRepository;
import com.TenaMed.inventory.repository.InventoryRepository;
import com.TenaMed.medicine.entity.Product;
import com.TenaMed.pharmacy.dto.request.CreateOrderFromCartRequest;
import com.TenaMed.pharmacy.dto.request.CreateOrderRequest;
import com.TenaMed.pharmacy.dto.response.OrderResponse;
import com.TenaMed.pharmacy.dto.response.UserOrderSummaryResponse;
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
import java.util.*;
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
    private final com.TenaMed.medicine.repository.ProductRepository productRepository;
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
                            com.TenaMed.medicine.repository.ProductRepository productRepository,
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
        this.productRepository = productRepository;
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
        order.setStatus(OrderStatus.PENDING);
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

        order.setAcceptedBy(actorUserId);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
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

        for (OrderItem item : order.getItems()) {
            inventoryService.releaseStock(
                order.getPharmacy().getId(),
                item.getProductId(),
                item.getQuantity(),
                order.getId()
            );
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
            order.setPaymentStatus(PaymentStatus.CONFIRMED);
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
        order.setStatus(OrderStatus.ACCEPTED);
        order.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);

        BigDecimal total = BigDecimal.ZERO;
        Set<OrderItem> orderItems = new LinkedHashSet<>();

        for (CreateOrderFromCartRequest.Item itemRequest : request.getItems()) {
            UUID productId = itemRequest.getProductId();
            Integer quantity = itemRequest.getQuantity();

            if (!inventoryService.checkAvailability(selectedPharmacyId, productId, quantity)) {
                throw new PharmacyValidationException("Insufficient stock for product " + productId);
            }

            UUID inventoryId = inventoryRepository.findByPharmacyIdAndProductId(selectedPharmacyId, productId)
                    .map(inv -> inv.getId())
                    .orElseThrow(() -> new PharmacyValidationException("Inventory not found for product " + productId));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setInventoryId(inventoryId);
            orderItem.setProductId(productId);
            
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new PharmacyValidationException("Product not found: " + productId));
            orderItem.setMedicineId(product.getMedicine().getId());

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
            boolean reserved = inventoryService.reserveStock(selectedPharmacyId, orderItem.getProductId(), orderItem.getQuantity(), savedOrder.getId());
            if (!reserved) {
                throw new PharmacyValidationException("Unable to reserve stock for product " + orderItem.getProductId());
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

    @Override
    @Transactional(readOnly = true)
    public List<UserOrderSummaryResponse> getCustomerOrders(UUID customerId) {
        if (customerId == null) {
            throw new PharmacyValidationException("customerId is required");
        }

        List<Order> orders = orderRepository.findWithItemsByCustomerId(customerId);
        if (orders.isEmpty()) {
            return List.of();
        }

        Set<UUID> productIds = new LinkedHashSet<>();
        Set<UUID> medicineIds = new LinkedHashSet<>();
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                if (item.getProductId() != null) {
                    productIds.add(item.getProductId());
                }
                if (item.getMedicineId() != null) {
                    medicineIds.add(item.getMedicineId());
                }
            }
        }

        Map<UUID, Product> productById = productRepository.findAllById(productIds)
            .stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<UUID, Medicine> medicineById = medicineRepository.findAllById(medicineIds)
            .stream()
            .collect(Collectors.toMap(Medicine::getId, Function.identity()));

        return orders.stream()
            .map(order -> toUserOrderSummary(order, productById, medicineById))
            .toList();
    }

    private Order fetchOrder(UUID orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private UserOrderSummaryResponse toUserOrderSummary(Order order,
                                                        Map<UUID, Product> productById,
                                                        Map<UUID, Medicine> medicineById) {
        LinkedHashSet<String> productNames = new LinkedHashSet<>();
        for (OrderItem item : order.getItems()) {
            String name = resolveProductName(item, productById, medicineById);
            if (name != null) {
                productNames.add(name);
            }
        }

        return UserOrderSummaryResponse.builder()
            .orderId(order.getId())
            .productNames(productNames.stream().toList())
            .totalPrice(order.getTotalAmount())
            .status(order.getStatus())
            .date(order.getCreatedAt())
            .build();
    }

    private String resolveProductName(OrderItem item,
                                      Map<UUID, Product> productById,
                                      Map<UUID, Medicine> medicineById) {
        Product product = productById.get(item.getProductId());
        String brandName = normalizeName(product == null ? null : product.getBrandName());
        if (brandName != null) {
            return brandName;
        }
        Medicine medicine = medicineById.get(item.getMedicineId());
        return normalizeName(medicine == null ? null : medicine.getName());
    }

    private String normalizeName(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateItemAvailability(UUID pharmacyId, List<OrderItem> items) {
        for (OrderItem item : items) {
            if (item.getProductId() == null) {
                throw new PharmacyValidationException("productId must never be null in Order operations");
            }
            boolean available = inventoryService.checkAvailability(pharmacyId, item.getProductId(), item.getQuantity());
            if (!available) {
                org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).error("Stock failure: Product {} unavailable in pharmacy {} for quantity {}", item.getProductId(), pharmacyId, item.getQuantity());
                throw new PharmacyValidationException("Insufficient stock for product " + item.getProductId());
            }
        }
    }

    @Override
    public java.util.List<PharmacyOrderResponse> getPharmacyOrders(UUID ownerId) {
        Optional<Pharmacy> pharmacy = pharmacyRepository.findByOwnerId(ownerId);
        if (pharmacy.isEmpty()) {
            throw new PharmacyNotFoundException("No pharmacy found for the given owner");
        }


        java.util.List<Order> orders = orderRepository.findByPharmacyId(pharmacy.get().getId());


        return orders.stream().map(this::mapToPharmacyOrderResponse).toList();
    }

    private PharmacyOrderResponse mapToPharmacyOrderResponse(Order order) {
        Prescription prescription = null;
        if (order.getPrescriptionId() != null) {
            prescription = prescriptionService.getPrescription(order.getPrescriptionId());

        }
        System.out.println( "pres"+prescription);
        java.util.List<PharmacyOrderResponse.PharmacyOrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> {
                    com.TenaMed.medicine.entity.Product product = productRepository.findById(item.getProductId()).orElse(null);
                    return PharmacyOrderResponse.PharmacyOrderItemResponse.builder()
                            .medicineName(product != null ? product.getBrandName() : "Unknown Product")
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .build();
                }).toList();

        String prescriptionImage = (prescription != null) ? prescription.getOriginalImages() : null;

        return PharmacyOrderResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .prescriptionImage(prescriptionImage)
                .orderItems(itemResponses)
                .type(prescription != null ? prescription.getType() : null)
                .highRisk(prescription != null ? prescription.getHighRisk() : null)
                .confidenceScore(prescription != null ? prescription.getConfidenceScore() : null)
                .totalAmount(order.getTotalAmount())
                .build();
    }

    private List<OrderItem> buildOrderItems(CreateOrderRequest request) {
        List<CreateOrderRequest.Item> items = request.getItems();
        List<UUID> prescriptionItemIds = items.stream().map(CreateOrderRequest.Item::getPrescriptionItemId).toList();
        List<PrescriptionItem> prescriptionItems = prescriptionItemRepository.findAllById(prescriptionItemIds);

        if (prescriptionItems.size() != prescriptionItemIds.size()) {
            throw new PharmacyValidationException("Some prescription item IDs do not exist");
        }

        Map<UUID, PrescriptionItem> byId = prescriptionItems.stream()
            .collect(Collectors.toMap(PrescriptionItem::getId, Function.identity()));

        return items.stream()
            .map(item -> toOrderItem(byId.get(item.getPrescriptionItemId()), item, request))
            .toList();
    }

    private OrderItem toOrderItem(PrescriptionItem prescriptionItem, CreateOrderRequest.Item itemRequest, CreateOrderRequest request) {
        if (prescriptionItem == null) {
            throw new PharmacyValidationException("Invalid prescription item reference");
        }
        if (itemRequest.getProductId() == null) {
            throw new PharmacyValidationException("productId must never be null in Order operations");
        }
        if (prescriptionItem.getQuantity() == null || prescriptionItem.getQuantity() <= 0) {
            throw new PharmacyValidationException("Prescription item quantity must be greater than zero");
        }
        if (request.getPrescriptionId() != null
            && !request.getPrescriptionId().equals(prescriptionItem.getPrescription().getId())) {
            throw new PharmacyValidationException("Prescription item does not belong to the provided prescriptionId");
        }

        UUID productId = itemRequest.getProductId();
        com.TenaMed.medicine.entity.Product product = productRepository.findById(productId)
            .orElseThrow(() -> new PharmacyValidationException("Product not found: " + productId));

        // CRITICAL: Prescription Safety Validation
        if (!product.getMedicine().getId().equals(prescriptionItem.getMedicine().getId())) {
            org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).warn("Prescription mismatch rejection: Product {} (medicine {}) does not match prescribed medicine {}", productId, product.getMedicine().getId(), prescriptionItem.getMedicine().getId());
            throw new PharmacyValidationException("Selected product does not match the prescribed medicine");
        }

        UUID inventoryId = inventoryRepository.findByPharmacyIdAndProductId(request.getPharmacyId(), productId)
            .map(inv -> inv.getId())
            .orElseThrow(() -> new PharmacyValidationException("Inventory not found for product " + productId));

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
        orderItem.setProductId(productId);
        orderItem.setMedicineId(product.getMedicine().getId());
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
                item.getProductId(),
                item.getQuantity(),
                order.getId()
            );
            if (!reserved) {
                throw new PharmacyValidationException("Unable to reserve stock for product " + item.getProductId());
            }
        }
    }
}