package com.TenaMed.pharmacy.service;

import com.TenaMed.pharmacy.dto.request.CreateOrderRequest;
import com.TenaMed.pharmacy.dto.request.CreateOrderFromCartRequest;
import com.TenaMed.pharmacy.dto.response.OrderResponse;
import com.TenaMed.pharmacy.dto.response.PharmacyOrderResponse;
import com.TenaMed.pharmacy.enums.PaymentStatus;
import com.TenaMed.pharmacy.enums.StaffRole;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request, UUID customerId);

    OrderResponse acceptOrder(java.util.UUID orderId, java.util.UUID actorUserId, StaffRole actorRole);

    OrderResponse rejectOrder(java.util.UUID orderId, String rejectionReason, java.util.UUID actorUserId, StaffRole actorRole);

    OrderResponse updatePaymentStatus(UUID orderId, PaymentStatus paymentStatus);

    OrderResponse createOrderFromCart(UUID customerId, CreateOrderFromCartRequest request);

List<PharmacyOrderResponse> getPharmacyOrders(UUID ownerId);
}