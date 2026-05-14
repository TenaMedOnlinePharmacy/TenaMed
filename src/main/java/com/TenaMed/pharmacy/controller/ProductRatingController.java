package com.TenaMed.pharmacy.controller;

import com.TenaMed.pharmacy.dto.request.ProductRatingRequest;
import com.TenaMed.pharmacy.dto.response.ProductRatingSummaryResponse;
import com.TenaMed.pharmacy.dto.response.ProductRatingUpsertResponse;
import com.TenaMed.pharmacy.exception.PharmacyException;
import com.TenaMed.pharmacy.exception.ProductRatingAuthorizationException;
import com.TenaMed.pharmacy.exception.ProductRatingNotFoundException;
import com.TenaMed.pharmacy.service.ProductRatingService;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/pharmacy/ratings")
public class ProductRatingController {

    private final ProductRatingService productRatingService;

    public ProductRatingController(ProductRatingService productRatingService) {
        this.productRatingService = productRatingService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> createOrUpdateRating(@Valid @RequestBody ProductRatingRequest request,
                                                  Principal principal) {
        UUID customerId = resolveCustomerId(principal);
        if (customerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        try {
            ProductRatingUpsertResponse response = productRatingService.createOrUpdateRating(customerId, request);
            return ResponseEntity.ok(response);
        } catch (PharmacyException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/inventory/{inventoryId}")
    public ResponseEntity<?> getRatingsForInventory(@PathVariable UUID inventoryId) {
        try {
            ProductRatingSummaryResponse response = productRatingService.getRatingsForInventory(inventoryId);
            return ResponseEntity.ok(response);
        } catch (PharmacyException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/{ratingId}")
    @PreAuthorize("hasAnyRole('ADMIN','PATIENT')")
    public ResponseEntity<?> deleteRating(@PathVariable UUID ratingId, Principal principal) {
        UUID actorUserId = resolveCustomerId(principal);
        if (actorUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        boolean isAdmin = hasRole("ROLE_ADMIN");
        try {
            productRatingService.deleteRating(ratingId, actorUserId, isAdmin);
            return ResponseEntity.noContent().build();
        } catch (ProductRatingNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        } catch (ProductRatingAuthorizationException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
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

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}
