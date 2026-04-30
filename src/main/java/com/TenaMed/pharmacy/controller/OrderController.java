package com.TenaMed.pharmacy.controller;

import com.TenaMed.pharmacy.dto.request.AcceptOrderRequest;
import com.TenaMed.pharmacy.dto.request.CreateOrderRequest;
import com.TenaMed.pharmacy.dto.request.RejectOrderRequest;
import com.TenaMed.pharmacy.dto.request.UpdatePaymentStatusRequest;
import com.TenaMed.pharmacy.dto.response.OrderResponse;
import com.TenaMed.pharmacy.enums.StaffRole;
import com.TenaMed.pharmacy.exception.PharmacyException;
import com.TenaMed.pharmacy.service.OrderService;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Collection;
import java.util.Locale;
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
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest request,
                                         Principal principal) {
        UUID customerId = resolveCustomerId(principal);
        if (customerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        try {
            OrderResponse response = orderService.createOrder(request, customerId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (PharmacyException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/accept")
    public ResponseEntity<?> acceptOrder(@Valid @RequestBody AcceptOrderRequest request,
                                         Principal principal) {
        UUID actorUserId = resolveCustomerId(principal);
        StaffRole actorRole = resolveStaffRole(principal);
        if (actorUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        if (actorRole == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Only pharmacy owner or pharmacist can accept orders"));
        }
        try {
            return ResponseEntity.ok(orderService.acceptOrder(request.getOrderId(), actorUserId, actorRole));
        } catch (PharmacyException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/reject")
    public ResponseEntity<?> reject (@Valid @RequestBody RejectOrderRequest request,
                                         Principal principal) {
        UUID actorUserId = resolveCustomerId(principal);
        StaffRole actorRole = resolveStaffRole(principal);
        if (actorUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        if (actorRole == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Only pharmacy owner or pharmacist can reject orders"));
        }
        try {
            return ResponseEntity.ok(orderService.rejectOrder(request.getOrderId(), request.getRejectionReason(), actorUserId, actorRole));
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

    @GetMapping("/pharmacyOrders")
    public ResponseEntity<?> getPharmacyOrders(Principal principal) {
        UUID ownerId = resolveCustomerId(principal);
        if (ownerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        try {

            return ResponseEntity.ok(orderService.getPharmacyOrders(ownerId));
        } catch (PharmacyException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    private UUID resolveCustomerId(Principal principal) {
        AuthenticatedUserPrincipal authenticatedUserPrincipal = resolveAuthenticatedPrincipal(principal);
        if (authenticatedUserPrincipal != null) {
            return authenticatedUserPrincipal.getUserId();
        }
        return null;
    }

    private StaffRole resolveStaffRole(Principal principal) {
        Collection<? extends GrantedAuthority> authorities = null;
        AuthenticatedUserPrincipal authenticatedUserPrincipal = resolveAuthenticatedPrincipal(principal);
        if (authenticatedUserPrincipal != null) {
            authorities = authenticatedUserPrincipal.getAuthorities();
        }

        if (authorities == null) {
            Authentication authentication = resolveAuthentication(principal);
            if (authentication != null) {
                authorities = authentication.getAuthorities();
            }
        }

        if (authorities == null) {
            return null;
        }

        for (GrantedAuthority authority : authorities) {
            if (authority == null || authority.getAuthority() == null) {
                continue;
            }
            String normalized = authority.getAuthority().trim().toUpperCase(Locale.ROOT);
            if ("ROLE_OWNER".equals(normalized)) {
                return StaffRole.OWNER;
            }
            if ("ROLE_PHARMACIST".equals(normalized)) {
                return StaffRole.PHARMACIST;
            }
            if ("ROLE_TECHNICIAN".equals(normalized)) {
                return StaffRole.TECHNICIAN;
            }
        }

        return null;
    }

    private AuthenticatedUserPrincipal resolveAuthenticatedPrincipal(Principal principal) {
        if (principal instanceof AuthenticatedUserPrincipal directPrincipal) {
            return directPrincipal;
        }
        Authentication authentication = resolveAuthentication(principal);
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal nestedPrincipal) {
            return nestedPrincipal;
        }
        return null;
    }

    private Authentication resolveAuthentication(Principal principal) {
        if (principal instanceof Authentication authentication) {
            return authentication;
        }
        Authentication contextAuth = SecurityContextHolder.getContext().getAuthentication();
        if (contextAuth != null && contextAuth.isAuthenticated()) {
            return contextAuth;
        }
        return null;
    }
}