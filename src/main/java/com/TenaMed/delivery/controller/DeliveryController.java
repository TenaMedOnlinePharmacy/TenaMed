package com.TenaMed.delivery.controller;

import com.TenaMed.delivery.dto.request.CreateDeliveryRequest;
import com.TenaMed.delivery.dto.request.DeliveryFailRequest;
import com.TenaMed.delivery.dto.response.DeliveryResponse;
import com.TenaMed.delivery.entity.Delivery;
import com.TenaMed.delivery.enums.DeliveryStatus;
import com.TenaMed.delivery.service.DeliveryService;
import com.TenaMed.user.entity.User;
import com.TenaMed.user.repository.UserRepository;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final UserRepository userRepository;

    public DeliveryController(DeliveryService deliveryService, UserRepository userRepository) {
        this.deliveryService = deliveryService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getDeliveries(@RequestParam("status") DeliveryStatus status) {
        List<Delivery> deliveries = deliveryService.getDeliveriesByStatus(status);
        return ResponseEntity.ok(toResponses(deliveries));
    }

    @GetMapping("/failed")
    public ResponseEntity<?> getFailedDeliveries() {
        List<Delivery> deliveries = deliveryService.getDeliveriesByStatus(DeliveryStatus.FAILED);
        return ResponseEntity.ok(toResponses(deliveries));
    }

    @PostMapping("/{id}/dispatch")
    public ResponseEntity<?> dispatchDelivery(@PathVariable("id") UUID deliveryId, Principal principal) {
        if (!isPharmacist(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Only pharmacist can dispatch deliveries"));
        }
        return ResponseEntity.ok(deliveryService.dispatchDelivery(deliveryId));
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<?> markDelivered(@PathVariable("id") UUID deliveryId, Principal principal) {
        if (!isPharmacist(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Only pharmacist can mark deliveries as delivered"));
        }
        return ResponseEntity.ok(deliveryService.markDelivered(deliveryId));
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<?> markFailed(@PathVariable("id") UUID deliveryId,
                                        @Valid @RequestBody DeliveryFailRequest request,
                                        Principal principal) {
        if (!isPharmacist(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Only pharmacist can mark deliveries as failed"));
        }
        return ResponseEntity.ok(deliveryService.markFailed(deliveryId, request.getReason()));
    }

    @PostMapping
    public ResponseEntity<?> createDelivery(@Valid @RequestBody CreateDeliveryRequest request,
                                            Principal principal) {
        UUID customerId = resolveCustomerId(principal);
        if (customerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Authentication required"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(deliveryService.createDelivery(request.getOrderId(), request.getDeliveryAddress()));
    }

    private UUID resolveCustomerId(Principal principal) {
        AuthenticatedUserPrincipal authenticatedUserPrincipal = resolveAuthenticatedPrincipal(principal);
        if (authenticatedUserPrincipal != null) {
            return authenticatedUserPrincipal.getUserId();
        }
        return null;
    }

    private List<DeliveryResponse> toResponses(List<Delivery> deliveries) {
        List<UUID> customerIds = deliveries.stream()
            .map(delivery -> delivery.getOrder().getCustomerId())
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();

        Map<UUID, String> phoneByCustomerId = userRepository.findAllById(customerIds)
            .stream()
            .collect(Collectors.toMap(User::getId, User::getPhone, (left, right) -> left));

        return deliveries.stream()
            .map(delivery -> toResponse(delivery, phoneByCustomerId.get(delivery.getOrder().getCustomerId())))
            .toList();
    }

    private DeliveryResponse toResponse(Delivery delivery, String customerPhone) {
        return DeliveryResponse.builder()
            .id(delivery.getId())
            .orderId(delivery.getOrder().getId())
            .status(delivery.getStatus())
            .deliveryAddress(delivery.getDeliveryAddress())
            .dispatchedAt(delivery.getDispatchedAt())
            .deliveredAt(delivery.getDeliveredAt())
            .failureReason(delivery.getFailureReason())
            .createdAt(delivery.getCreatedAt())
            .customerPhone(customerPhone)
            .build();
    }

    private boolean isPharmacist(Principal principal) {
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
            return false;
        }

        for (GrantedAuthority authority : authorities) {
            if (authority == null || authority.getAuthority() == null) {
                continue;
            }
            String normalized = authority.getAuthority().trim().toUpperCase(Locale.ROOT);
            if ("ROLE_PHARMACIST".equals(normalized)) {
                return true;
            }
        }

        return false;
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
