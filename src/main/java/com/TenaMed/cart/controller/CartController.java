package com.TenaMed.cart.controller;

import com.TenaMed.cart.dto.request.AddCartItemRequest;
import com.TenaMed.cart.dto.request.UpdateCartItemQuantityRequest;
import com.TenaMed.cart.dto.response.CartResponse;
import com.TenaMed.cart.dto.response.CheckoutCartResponse;
import com.TenaMed.cart.exception.CartException;
import com.TenaMed.cart.service.CartService;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    public ResponseEntity<?> addItem(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                     @Valid @RequestBody AddCartItemRequest request) {
        UUID userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        try {
            CartResponse response = cartService.addItem(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (CartException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getCart(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        UUID userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateQuantity(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                            @PathVariable UUID itemId,
                                            @Valid @RequestBody UpdateCartItemQuantityRequest request) {
        UUID userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        try {
            return ResponseEntity.ok(cartService.updateItemQuantity(userId, itemId, request));
        } catch (CartException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> removeItem(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                        @PathVariable UUID itemId) {
        UUID userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        try {
            return ResponseEntity.ok(cartService.removeItem(userId, itemId));
        } catch (CartException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        UUID userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        return ResponseEntity.ok(cartService.clearCart(userId));
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        UUID userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        try {
            CheckoutCartResponse response = cartService.checkout(userId);
            System.out.println(response);
            return ResponseEntity.ok(response);
        } catch (CartException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    private UUID resolveUserId(AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            return null;
        }
        return principal.getUserId();
    }
}
