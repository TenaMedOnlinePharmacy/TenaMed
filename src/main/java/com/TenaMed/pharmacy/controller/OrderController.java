package com.TenaMed.pharmacy.controller;

import com.TenaMed.pharmacy.dto.request.AcceptOrderRequest;
import com.TenaMed.pharmacy.dto.request.CreateOrderRequest;
import com.TenaMed.pharmacy.dto.request.RejectOrderRequest;
import com.TenaMed.pharmacy.dto.request.UpdatePaymentStatusRequest;
import com.TenaMed.pharmacy.dto.response.OrderResponse;
import com.TenaMed.pharmacy.exception.PharmacyException;
import com.TenaMed.pharmacy.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        try {
            OrderResponse response = orderService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (PharmacyException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> acceptOrder(@PathVariable UUID id,
                                         @Valid @RequestBody AcceptOrderRequest request) {
        try {
            return ResponseEntity.ok(orderService.acceptOrder(id, request.getActorUserId(), request.getActorRole()));
        } catch (PharmacyException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectOrder(@PathVariable UUID id,
                                         @Valid @RequestBody RejectOrderRequest request) {
        try {
            return ResponseEntity.ok(orderService.rejectOrder(id, request.getRejectionReason()));
        } catch (PharmacyException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/payment-status")
    public ResponseEntity<?> updatePaymentStatus(@PathVariable UUID id,
                                                 @Valid @RequestBody UpdatePaymentStatusRequest request) {
        try {
            return ResponseEntity.ok(orderService.updatePaymentStatus(id, request.getPaymentStatus()));
        } catch (PharmacyException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }
}