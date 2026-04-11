package com.TenaMed.inventory.controller;

import com.TenaMed.inventory.dto.AddBatchRequest;
import com.TenaMed.inventory.dto.BatchResponse;
import com.TenaMed.inventory.dto.CreateInventoryRequest;
import com.TenaMed.inventory.dto.InventoryResponse;
import com.TenaMed.inventory.dto.StockActionRequest;
import com.TenaMed.inventory.exception.DuplicateInventoryException;
import com.TenaMed.inventory.exception.InventoryException;
import com.TenaMed.inventory.exception.InventoryNotFoundException;
import com.TenaMed.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/batch")
    public ResponseEntity<?> addBatch(@Valid @RequestBody AddBatchRequest request) {
        try {
            BatchResponse response = inventoryService.addBatch(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (InventoryNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        } catch (InventoryException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getInventory(@RequestParam UUID pharmacyId,
                                          @RequestParam UUID medicineId) {
        try {
            return ResponseEntity.ok(inventoryService.getInventory(pharmacyId, medicineId));
        } catch (InventoryNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkAvailability(@RequestParam UUID pharmacyId,
                                                                   @RequestParam UUID medicineId,
                                                                   @RequestParam Integer quantity) {
        boolean available = inventoryService.checkAvailability(pharmacyId, medicineId, quantity);
        return ResponseEntity.ok(Map.of("available", available));
    }

    @PostMapping("/reserve")
    public ResponseEntity<Map<String, Boolean>> reserve(@Valid @RequestBody StockActionRequest request) {
        boolean reserved = inventoryService.reserveStock(
            request.getPharmacyId(),
            request.getMedicineId(),
            request.getQuantity(),
            request.getReferenceId()
        );
        return ResponseEntity.ok(Map.of("reserved", reserved));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Boolean>> confirm(@Valid @RequestBody StockActionRequest request) {
        boolean confirmed = inventoryService.confirmStock(
            request.getPharmacyId(),
            request.getMedicineId(),
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
                request.getMedicineId(),
                request.getQuantity(),
                request.getReferenceId()
            );
            return ResponseEntity.ok(Map.of("released", true));
        } catch (InventoryException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        }
    }
}