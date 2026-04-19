package com.TenaMed.pharmacy.service;

import com.TenaMed.pharmacy.dto.request.CreateOrderRequest;
import com.TenaMed.pharmacy.dto.request.CreateOrderFromCartRequest;
import com.TenaMed.pharmacy.dto.response.OrderResponse;
import com.TenaMed.pharmacy.enums.PaymentStatus;
import com.TenaMed.pharmacy.enums.StaffRole;

import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request, UUID customerId);

    OrderResponse acceptOrder(UUID orderId, UUID actorUserId, StaffRole actorRole);

    OrderResponse rejectOrder(UUID orderId, String rejectionReason, UUID actorUserId, StaffRole actorRole);

    OrderResponse updatePaymentStatus(UUID orderId, PaymentStatus paymentStatus);

    OrderResponse createOrderFromCart(UUID customerId, CreateOrderFromCartRequest request);
}