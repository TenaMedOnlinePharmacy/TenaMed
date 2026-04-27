package com.TenaMed.inventory.controller;

import com.TenaMed.inventory.dto.AddBatchRequest;
import com.TenaMed.inventory.dto.BatchResponse;
import com.TenaMed.inventory.dto.CreateInventoryRequest;
import com.TenaMed.inventory.dto.InventoryListItemResponse;
import com.TenaMed.inventory.dto.InventoryResponse;
import com.TenaMed.inventory.dto.StockActionRequest;
import com.TenaMed.inventory.exception.DuplicateInventoryException;
import com.TenaMed.inventory.exception.BatchNotFoundException;
import com.TenaMed.inventory.exception.InventoryException;
import com.TenaMed.inventory.exception.InventoryNotFoundException;
import com.TenaMed.inventory.service.InventoryService;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<?> createInventory(@Valid @RequestBody CreateInventoryRequest request) {
        try {
            InventoryResponse response = inventoryService.createInventory(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (DuplicateInventoryException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
        } catch (InventoryException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addBatch(@Valid @RequestPart("batch") AddBatchRequest request,
                                      @RequestPart(value = "image", required = false) MultipartFile image,
                                      Principal principal) {
        UUID actorUserId = resolveCustomerId(principal);
        if (actorUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        try {
            BatchResponse response = inventoryService.addBatch(request, actorUserId, image);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (InventoryNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        } catch (InventoryException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getInventory(@RequestParam UUID pharmacyId,
                                          @RequestParam UUID productId) {
        try {
            return ResponseEntity.ok(inventoryService.getInventory(pharmacyId, productId));
        } catch (InventoryNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> getCurrentUserInventoryList(Principal principal) {
        UUID actorUserId = resolveCustomerId(principal);
        if (actorUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        try {
            List<InventoryListItemResponse> response = inventoryService.getCurrentUserInventoryList(actorUserId);
            return ResponseEntity.ok(response);
        } catch (InventoryException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkAvailability(@RequestParam UUID pharmacyId,
                                                                   @RequestParam UUID productId,
                                                                   @RequestParam Integer quantity) {
        boolean available = inventoryService.checkAvailability(pharmacyId, productId, quantity);
        return ResponseEntity.ok(Map.of("available", available));
    }

    @DeleteMapping("/batch/{batchId}")
    public ResponseEntity<?> deleteBatch(@PathVariable UUID batchId, Principal principal) {
        UUID actorUserId = resolveCustomerId(principal);
        if (actorUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            inventoryService.deleteBatch(batchId, actorUserId);
            return ResponseEntity.noContent().build();
        } catch (BatchNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (InventoryException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/reserve")
    public ResponseEntity<Map<String, Boolean>> reserve(@Valid @RequestBody StockActionRequest request) {
        boolean reserved = inventoryService.reserveStock(
            request.getPharmacyId(),
            request.getProductId(),
            request.getQuantity(),
            request.getReferenceId()
        );
        return ResponseEntity.ok(Map.of("reserved", reserved));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Boolean>> confirm(@Valid @RequestBody StockActionRequest request) {
        boolean confirmed = inventoryService.confirmStock(
            request.getPharmacyId(),
            request.getProductId(),
            request.getQuantity(),
            request.getReferenceId()
        );
        return ResponseEntity.ok(Map.of("confirmed", confirmed));
    }

    @PostMapping("/release")
    public ResponseEntity<?> release(@Valid @RequestBody StockActionRequest request) {
        try {
            inventoryService.releaseStock(
                request.getPharmacyId(),
                request.getProductId(),
                request.getQuantity(),
                request.getReferenceId()
            );
            return ResponseEntity.ok(Map.of("released", true));
        } catch (InventoryException ex) {
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
}