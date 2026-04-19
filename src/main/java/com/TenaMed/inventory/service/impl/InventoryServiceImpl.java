package com.TenaMed.inventory.service.impl;

import com.TenaMed.inventory.dto.AddBatchRequest;
import com.TenaMed.inventory.dto.BatchResponse;
import com.TenaMed.inventory.dto.CreateInventoryRequest;
import com.TenaMed.inventory.dto.InventoryResponse;
import com.TenaMed.inventory.entity.Batch;
import com.TenaMed.inventory.entity.Inventory;
import com.TenaMed.inventory.entity.StockMovement;
import com.TenaMed.inventory.enums.BatchStatus;
import com.TenaMed.inventory.enums.StockMovementType;
import com.TenaMed.inventory.exception.DuplicateInventoryException;
import com.TenaMed.inventory.exception.InventoryNotFoundException;
import com.TenaMed.inventory.exception.InventoryValidationException;
import com.TenaMed.inventory.mapper.BatchMapper;
import com.TenaMed.inventory.mapper.InventoryMapper;
import com.TenaMed.inventory.repository.BatchRepository;
import com.TenaMed.inventory.repository.InventoryRepository;
import com.TenaMed.inventory.repository.StockMovementRepository;
import com.TenaMed.inventory.service.InventoryService;
import com.TenaMed.events.DomainEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final BatchRepository batchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryMapper inventoryMapper;
    private final BatchMapper batchMapper;
    private final DomainEventService domainEventService;

    public InventoryServiceImpl(InventoryRepository inventoryRepository,
                                BatchRepository batchRepository,
                                StockMovementRepository stockMovementRepository,
                                InventoryMapper inventoryMapper,
                                BatchMapper batchMapper,
                                DomainEventService domainEventService) {
        this.inventoryRepository = inventoryRepository;
        this.batchRepository = batchRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.inventoryMapper = inventoryMapper;
        this.batchMapper = batchMapper;
        this.domainEventService = domainEventService;
    }

    @Override
    public InventoryResponse createInventory(CreateInventoryRequest request) {
        validateCreateRequest(request);

        inventoryRepository.findByPharmacyIdAndMedicineId(request.getPharmacyId(), request.getMedicineId())
            .ifPresent(existing -> {
                throw new DuplicateInventoryException(request.getPharmacyId(), request.getMedicineId());
            });

        Inventory saved = inventoryRepository.save(inventoryMapper.toEntity(request));
        domainEventService.publish(
            "INVENTORY_CREATED",
            "INVENTORY",
            saved.getId(),
            "PHARMACY",
            saved.getPharmacyId(),
            Map.of("medicineId", saved.getMedicineId().toString())
        );
        return inventoryMapper.toResponse(saved, List.of());
    }

    @Override
    public BatchResponse addBatch(AddBatchRequest request) {
        validateAddBatchRequest(request);

        Inventory inventory = inventoryRepository.findById(request.getInventoryId())
            .orElseThrow(() -> new InventoryNotFoundException(request.getInventoryId()));
        int oldTotalQuantity = inventory.getTotalQuantity();

        Batch savedBatch = batchRepository.save(batchMapper.toEntity(request, inventory));

        inventory.setTotalQuantity(inventory.getTotalQuantity() + request.getQuantity());
        inventoryRepository.save(inventory);

        saveMovement(inventory.getId(), StockMovementType.IN, request.getQuantity(), savedBatch.getId());

        domainEventService.publish(
            "INVENTORY_BATCH_ADDED",
            "INVENTORY",
            inventory.getId(),
            "PHARMACY",
            inventory.getPharmacyId(),
            Map.of(
                "batchId", savedBatch.getId().toString(),
                "changes", Map.of("totalQuantity", Map.of("old", oldTotalQuantity, "new", inventory.getTotalQuantity()))
            )
        );

        return batchMapper.toResponse(savedBatch);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventory(UUID pharmacyId, UUID medicineId) {
        Inventory inventory = inventoryRepository.findByPharmacyIdAndMedicineId(pharmacyId, medicineId)
            .orElseThrow(() -> new InventoryNotFoundException(pharmacyId, medicineId));

        List<BatchResponse> batches = batchRepository.findByInventoryIdOrderByExpiryDateAsc(inventory.getId())
            .stream()
            .map(batchMapper::toResponse)
            .toList();

        return inventoryMapper.toResponse(inventory, batches);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkAvailability(UUID pharmacyId, UUID medicineId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return false;
        }
        return inventoryRepository.findByPharmacyIdAndMedicineId(pharmacyId, medicineId)
            .map(inventory -> (inventory.getTotalQuantity() - inventory.getReservedQuantity()) >= quantity)
            .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkAvailability(UUID medicineId, Integer quantity) {
        if (medicineId == null || quantity == null || quantity <= 0) {
            return false;
        }
        return inventoryRepository.existsAvailableByMedicineId(medicineId, quantity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findPharmacyIdsWithAvailableMedicine(UUID medicineId, Integer quantity) {
        if (medicineId == null || quantity == null || quantity <= 0) {
            return List.of();
        }
        return inventoryRepository.findPharmacyIdsWithAvailableMedicine(medicineId, quantity);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal resolveUnitPrice(UUID medicineId) {
        if (medicineId == null) {
            return null;
        }

        return inventoryRepository.findByMedicineIdIn(List.of(medicineId)).stream()
                .flatMap(inventory -> batchRepository.findByInventoryIdAndStatusOrderByExpiryDateAsc(inventory.getId(), BatchStatus.ACTIVE).stream())
                .filter(batch -> batch.getQuantity() != null && batch.getQuantity() > 0)
                .map(Batch::getSellingPrice)
                .filter(price -> price != null)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    @Override
    public boolean reserveStock(UUID pharmacyId, UUID medicineId, Integer quantity) {
        return reserveStock(pharmacyId, medicineId, quantity, null);
    }

    @Override
    public boolean reserveStock(UUID pharmacyId, UUID medicineId, Integer quantity, UUID referenceId) {
        if (quantity == null || quantity <= 0) {
            return false;
        }

        Inventory inventory = inventoryRepository.findWithLockByPharmacyIdAndMedicineId(pharmacyId, medicineId)
            .orElse(null);

        if (inventory == null) {
            return false;
        }

        int available = inventory.getTotalQuantity() - inventory.getReservedQuantity();
        if (available < quantity) {
            return false;
        }

        int oldReservedQuantity = inventory.getReservedQuantity();
        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventoryRepository.save(inventory);
        saveMovement(inventory.getId(), StockMovementType.RESERVE, quantity, referenceId);
        domainEventService.publish(
            "INVENTORY_STOCK_RESERVED",
            "INVENTORY",
            inventory.getId(),
            "SYSTEM",
            null,
            "PHARMACY",
            inventory.getPharmacyId(),
            Map.of(
                "medicineId", medicineId.toString(),
                "changes", Map.of("reservedQuantity", Map.of("old", oldReservedQuantity, "new", inventory.getReservedQuantity()))
            )
        );
        return true;
    }

    @Override
    public boolean confirmStock(UUID pharmacyId, UUID medicineId, Integer quantity, UUID referenceId) {
        if (quantity == null || quantity <= 0) {
            return false;
        }

        Inventory inventory = inventoryRepository.findWithLockByPharmacyIdAndMedicineId(pharmacyId, medicineId)
            .orElse(null);

        if (inventory == null || inventory.getReservedQuantity() < quantity || inventory.getTotalQuantity() < quantity) {
            return false;
        }

        List<Batch> activeBatches = batchRepository.findByInventoryIdAndStatusOrderByExpiryDateAsc(inventory.getId(), BatchStatus.ACTIVE);
        int remaining = quantity;

        for (Batch batch : activeBatches) {
            if (remaining == 0) {
                break;
            }
            int consumed = Math.min(batch.getQuantity(), remaining);
            batch.setQuantity(batch.getQuantity() - consumed);
            if (batch.getQuantity() == 0 && batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(LocalDate.now())) {
                batch.setStatus(BatchStatus.EXPIRED);
            }
            remaining -= consumed;
        }

        if (remaining > 0) {
            return false;
        }

        batchRepository.saveAll(activeBatches);

        int oldReservedQuantity = inventory.getReservedQuantity();
        int oldTotalQuantity = inventory.getTotalQuantity();
        inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
        inventory.setTotalQuantity(inventory.getTotalQuantity() - quantity);
        inventoryRepository.save(inventory);
        saveMovement(inventory.getId(), StockMovementType.OUT, quantity, referenceId);
        domainEventService.publish(
            "INVENTORY_STOCK_CONFIRMED",
            "INVENTORY",
            inventory.getId(),
            "SYSTEM",
            null,
            "PHARMACY",
            inventory.getPharmacyId(),
            Map.of(
                "medicineId", medicineId.toString(),
                "changes", Map.of(
                    "reservedQuantity", Map.of("old", oldReservedQuantity, "new", inventory.getReservedQuantity()),
                    "totalQuantity", Map.of("old", oldTotalQuantity, "new", inventory.getTotalQuantity())
                )
            )
        );
        return true;
    }

    @Override
    public void releaseStock(UUID pharmacyId, UUID medicineId, Integer quantity) {
        releaseStock(pharmacyId, medicineId, quantity, null);
    }

    @Override
    public void releaseStock(UUID pharmacyId, UUID medicineId, Integer quantity, UUID referenceId) {
        if (quantity == null || quantity <= 0) {
            return;
        }

        Inventory inventory = inventoryRepository.findWithLockByPharmacyIdAndMedicineId(pharmacyId, medicineId)
            .orElseThrow(() -> new InventoryNotFoundException(pharmacyId, medicineId));

        if (inventory.getReservedQuantity() < quantity) {
            throw new InventoryValidationException("Cannot release more than reserved quantity");
        }

        int oldReservedQuantity = inventory.getReservedQuantity();
        inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
        inventoryRepository.save(inventory);
        saveMovement(inventory.getId(), StockMovementType.RELEASE, quantity, referenceId);
        domainEventService.publish(
            "INVENTORY_STOCK_RELEASED",
            "INVENTORY",
            inventory.getId(),
            "SYSTEM",
            null,
            "PHARMACY",
            inventory.getPharmacyId(),
            Map.of(
                "medicineId", medicineId.toString(),
                "changes", Map.of("reservedQuantity", Map.of("old", oldReservedQuantity, "new", inventory.getReservedQuantity()))
            )
        );
    }

    private void saveMovement(UUID inventoryId, StockMovementType type, Integer quantity, UUID referenceId) {
        StockMovement movement = new StockMovement();
        movement.setInventoryId(inventoryId);
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setReferenceId(referenceId);
        stockMovementRepository.save(movement);
    }

    private void validateCreateRequest(CreateInventoryRequest request) {
        if (request.getTotalQuantity() == null || request.getTotalQuantity() < 0) {
            throw new InventoryValidationException("totalQuantity must be >= 0");
        }
        if (request.getReservedQuantity() != null && request.getReservedQuantity() < 0) {
            throw new InventoryValidationException("reservedQuantity must be >= 0");
        }
        if (request.getReservedQuantity() != null && request.getReservedQuantity() > request.getTotalQuantity()) {
            throw new InventoryValidationException("reservedQuantity cannot exceed totalQuantity");
        }
    }

    private void validateAddBatchRequest(AddBatchRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new InventoryValidationException("quantity must be > 0");
        }
        if (request.getExpiryDate() != null && request.getManufacturingDate() != null
            && request.getExpiryDate().isBefore(request.getManufacturingDate())) {
            throw new InventoryValidationException("expiryDate cannot be before manufacturingDate");
        }
    }
}