package com.TenaMed.pharmacy.service.impl;

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
import com.TenaMed.pharmacy.exception.OrderNotFoundException;
import com.TenaMed.pharmacy.exception.PharmacyNotFoundException;
import com.TenaMed.pharmacy.exception.PharmacyValidationException;
import com.TenaMed.pharmacy.integration.InventoryAdapter;
import com.TenaMed.pharmacy.integration.PrescriptionAdapter;
import com.TenaMed.pharmacy.mapper.OrderMapper;
import com.TenaMed.pharmacy.repository.OrderItemRepository;
import com.TenaMed.pharmacy.repository.OrderRepository;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
import com.TenaMed.pharmacy.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PharmacyRepository pharmacyRepository;
    private final OrderMapper orderMapper;
    private final InventoryAdapter inventoryAdapter;
    private final PrescriptionAdapter prescriptionAdapter;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            PharmacyRepository pharmacyRepository,
                            OrderMapper orderMapper,
                            InventoryAdapter inventoryAdapter,
                            PrescriptionAdapter prescriptionAdapter) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.orderMapper = orderMapper;
        this.inventoryAdapter = inventoryAdapter;
        this.prescriptionAdapter = prescriptionAdapter;
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        Pharmacy pharmacy = pharmacyRepository.findById(request.getPharmacyId())
            .orElseThrow(() -> new PharmacyNotFoundException(request.getPharmacyId()));

        if (pharmacy.getStatus() != PharmacyStatus.VERIFIED) {
            throw new PharmacyValidationException("Pharmacy must be VERIFIED before placing orders");
        }

        if (request.getPrescriptionId() != null) {
            prescriptionAdapter.getPrescription(request.getPrescriptionId());
        }

        validateItemAvailability(request.getPharmacyId(), request.getItems());

        Order order = orderMapper.toEntity(request, pharmacy);
        order.setStatus(OrderStatus.PENDING_REVIEW);
        Order savedOrder = orderRepository.save(order);

        List<OrderItem> items = request.getItems().stream()
            .map(item -> orderMapper.toOrderItemEntity(item, savedOrder))
            .toList();
        List<OrderItem> savedItems = orderItemRepository.saveAll(items);
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

    private void validateItemAvailability(UUID pharmacyId, List<OrderItemRequest> items) {
        for (OrderItemRequest item : items) {
            boolean available = inventoryAdapter.checkAvailability(pharmacyId, item.getMedicineId(), item.getQuantity());
            if (!available) {
                throw new PharmacyValidationException("Insufficient stock for medicine " + item.getMedicineId());
            }
        }
    }

    private void reserveOrderItems(Order order) {
        for (OrderItem item : order.getItems()) {
            boolean reserved = inventoryAdapter.reserveStock(
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