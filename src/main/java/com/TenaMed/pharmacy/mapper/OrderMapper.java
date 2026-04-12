package com.TenaMed.pharmacy.mapper;

import com.TenaMed.pharmacy.dto.request.CreateOrderRequest;
import com.TenaMed.pharmacy.dto.request.OrderItemRequest;
import com.TenaMed.pharmacy.dto.response.OrderItemResponse;
import com.TenaMed.pharmacy.dto.response.OrderResponse;
import com.TenaMed.pharmacy.entity.Order;
import com.TenaMed.pharmacy.entity.OrderItem;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.enums.OrderStatus;
import com.TenaMed.pharmacy.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OrderMapper {

    public Order toEntity(CreateOrderRequest request, Pharmacy pharmacy, java.util.UUID customerId) {
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setPharmacy(pharmacy);
        order.setPrescriptionId(request.getPrescriptionId());
        order.setStatus(OrderStatus.PENDING_REVIEW);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);
        return order;
    }

    public OrderItem toOrderItemEntity(OrderItemRequest request, Order order) {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setInventoryId(request.getInventoryId());
        item.setMedicineId(request.getMedicineId());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(request.getUnitPrice());
        return item;
    }

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
            .map(this::toOrderItemResponse)
            .toList();

        return OrderResponse.builder()
            .id(order.getId())
            .customerId(order.getCustomerId())
            .pharmacyId(order.getPharmacy().getId())
            .prescriptionId(order.getPrescriptionId())
            .status(order.getStatus())
            .paymentStatus(order.getPaymentStatus())
            .totalAmount(order.getTotalAmount())
            .rejectionReason(order.getRejectionReason())
            .acceptedBy(order.getAcceptedBy())
            .items(items)
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .build();
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
            .id(item.getId())
            .inventoryId(item.getInventoryId())
            .medicineId(item.getMedicineId())
            .quantity(item.getQuantity())
            .unitPrice(item.getUnitPrice())
            .build();
    }

}