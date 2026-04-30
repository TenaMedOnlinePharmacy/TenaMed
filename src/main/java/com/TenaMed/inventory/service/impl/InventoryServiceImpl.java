package com.TenaMed.inventory.service.impl;

import com.TenaMed.inventory.dto.AddBatchRequest;
import com.TenaMed.inventory.dto.BatchEditDetailsResponse;
import com.TenaMed.inventory.dto.BatchResponse;
import com.TenaMed.inventory.dto.CreateInventoryRequest;
import com.TenaMed.inventory.dto.InventoryListItemResponse;
import com.TenaMed.inventory.dto.InventoryResponse;
import com.TenaMed.inventory.entity.Batch;
import com.TenaMed.inventory.entity.Inventory;
import com.TenaMed.inventory.entity.StockMovement;
import com.TenaMed.inventory.enums.BatchStatus;
import com.TenaMed.inventory.enums.StockMovementType;
import com.TenaMed.inventory.exception.DuplicateInventoryException;
import com.TenaMed.inventory.exception.BatchNotFoundException;
import com.TenaMed.inventory.exception.InventoryNotFoundException;
import com.TenaMed.inventory.exception.InventoryValidationException;
import com.TenaMed.inventory.mapper.BatchMapper;
import com.TenaMed.inventory.mapper.InventoryMapper;
import com.TenaMed.inventory.repository.BatchRepository;
import com.TenaMed.inventory.repository.InventoryRepository;
import com.TenaMed.inventory.repository.StockMovementRepository;
import com.TenaMed.inventory.service.InventoryService;
import com.TenaMed.events.DomainEventService;
import com.TenaMed.medicine.entity.Medicine;
import com.TenaMed.medicine.entity.Product;
import com.TenaMed.medicine.repository.MedicineRepository;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.entity.UserPharmacy;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
import com.TenaMed.pharmacy.repository.UserPharmacyRepository;
import com.TenaMed.ocr.service.SupabaseStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final BatchRepository batchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryMapper inventoryMapper;
    private final BatchMapper batchMapper;
    private final DomainEventService domainEventService;
    private final com.TenaMed.medicine.repository.ProductRepository productRepository;
    private final MedicineRepository medicineRepository;
    private final PharmacyRepository pharmacyRepository;
    private final UserPharmacyRepository userPharmacyRepository;
    private final SupabaseStorageService supabaseStorageService;

    private static final String PRODUCT_IMAGE_FOLDER = "products";

    public InventoryServiceImpl(InventoryRepository inventoryRepository,
                                BatchRepository batchRepository,
                                StockMovementRepository stockMovementRepository,
                                InventoryMapper inventoryMapper,
                                BatchMapper batchMapper,
                                DomainEventService domainEventService,
                                com.TenaMed.medicine.repository.ProductRepository productRepository,
                                MedicineRepository medicineRepository,
                                PharmacyRepository pharmacyRepository,
                                UserPharmacyRepository userPharmacyRepository,
                                SupabaseStorageService supabaseStorageService) {
        this.inventoryRepository = inventoryRepository;
        this.batchRepository = batchRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.inventoryMapper = inventoryMapper;
        this.batchMapper = batchMapper;
        this.domainEventService = domainEventService;
        this.productRepository = productRepository;
        this.medicineRepository = medicineRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.userPharmacyRepository = userPharmacyRepository;
        this.supabaseStorageService = supabaseStorageService;
    }

    @Override
    public InventoryResponse createInventory(CreateInventoryRequest request) {
        validateCreateRequest(request);

        inventoryRepository.findByPharmacyIdAndProductId(request.getPharmacyId(), request.getProductId())
            .ifPresent(existing -> {
                throw new DuplicateInventoryException(request.getPharmacyId(), request.getProductId());
            });

        Inventory toSave = inventoryMapper.toEntity(request);
        if (toSave.getMedicineId() == null) {
            Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new InventoryValidationException("Product not found: " + request.getProductId()));
            if (product.getMedicine() == null || product.getMedicine().getId() == null) {
                throw new InventoryValidationException("Product has no medicine mapping: " + request.getProductId());
            }
            toSave.setMedicineId(product.getMedicine().getId());
        }

        Inventory saved = inventoryRepository.save(toSave);
        domainEventService.publish(
            "INVENTORY_CREATED",
            "INVENTORY",
            saved.getId(),
            "PHARMACY",
            saved.getPharmacyId(),
            Map.of("productId", saved.getProductId().toString())
        );
        return inventoryMapper.toResponse(saved, List.of());
    }

    @Override
    public BatchResponse addBatch(AddBatchRequest request, java.util.UUID actorUserId, MultipartFile image) {
        validateAddBatchRequest(request);

        com.TenaMed.pharmacy.entity.Pharmacy pharmacy = resolvePharmacyForActor(actorUserId);
        Product product = resolveOrCreateProduct(request);

        if (image != null && !image.isEmpty()) {
            String objectPath = supabaseStorageService.uploadAndGetObjectPath(image, PRODUCT_IMAGE_FOLDER);
//            product.setImageUrl(supabaseStorageService.resolveSignedUrl(objectPath));
            product.setImageUrl(objectPath);
            productRepository.save(product);
        }

        if (product.getMedicine() == null || product.getMedicine().getId() == null) {
            throw new InventoryValidationException("Product has no medicine mapping: " + product.getId());
        }

        Inventory inventory = inventoryRepository.findByPharmacyIdAndProductId(pharmacy.getId(), product.getId())
                .orElseGet(() -> inventoryRepository.save(buildNewInventory(
                    pharmacy.getId(),
                    product.getId(),
                    product.getMedicine().getId(),
                    request.getReorderLevel()
                )));

        int oldTotalQuantity = inventory.getTotalQuantity();

        Batch batch = batchMapper.toEntity(request, inventory);
        batch.setStatus(BatchStatus.ACTIVE);
        Batch savedBatch = batchRepository.save(batch);

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
    public BatchEditDetailsResponse getBatchForEdit(UUID batchId, UUID actorUserId) {
        if (batchId == null) {
            throw new InventoryValidationException("batchId is required");
        }

        Pharmacy pharmacy = resolvePharmacyForActor(actorUserId);
        Batch batch = batchRepository.findByIdAndInventoryPharmacyId(batchId, pharmacy.getId())
            .orElseThrow(() -> new BatchNotFoundException(batchId));

        Inventory inventory = batch.getInventory();
        Product product = productRepository.findById(inventory.getProductId())
            .orElseThrow(() -> new InventoryValidationException("Product not found for batch: " + batchId));
        Medicine medicine = product.getMedicine();

        AddBatchRequest responseRequest = new AddBatchRequest();
        responseRequest.setProductId(product.getId());
        responseRequest.setMedicineName(medicine != null ? medicine.getName() : null);
        responseRequest.setBrandName(product.getBrandName());
        responseRequest.setManufacturer(product.getManufacturer());
        responseRequest.setBatchNumber(batch.getBatchNumber());
        responseRequest.setManufacturingDate(batch.getManufacturingDate());
        responseRequest.setExpiryDate(batch.getExpiryDate());
        responseRequest.setQuantity(batch.getQuantity());
        responseRequest.setUnitCost(batch.getUnitCost());
        responseRequest.setSellingPrice(batch.getSellingPrice());
        responseRequest.setReorderLevel(inventory.getReorderLevel());

        return BatchEditDetailsResponse.builder()
            .batchId(batch.getId())
            .batch(responseRequest)
            .imageUrl(product.getImageUrl())
            .build();
    }

    @Override
    public BatchResponse editBatch(UUID batchId, AddBatchRequest request, UUID actorUserId, MultipartFile image) {
        if (batchId == null) {
            throw new InventoryValidationException("batchId is required");
        }
        validateAddBatchRequest(request);

        Pharmacy pharmacy = resolvePharmacyForActor(actorUserId);
        Batch batch = batchRepository.findByIdAndInventoryPharmacyId(batchId, pharmacy.getId())
            .orElseThrow(() -> new BatchNotFoundException(batchId));

        Inventory currentInventory = batch.getInventory();
        Product product = resolveOrCreateProduct(request);

        if (image != null && !image.isEmpty()) {
            String objectPath = supabaseStorageService.uploadAndGetObjectPath(image, PRODUCT_IMAGE_FOLDER);
//            product.setImageUrl(supabaseStorageService.resolveSignedUrl(objectPath));
            product.setImageUrl(objectPath);
            productRepository.save(product);
        }

        if (product.getMedicine() == null || product.getMedicine().getId() == null) {
            throw new InventoryValidationException("Product has no medicine mapping: " + product.getId());
        }

        Inventory targetInventory = inventoryRepository.findByPharmacyIdAndProductId(pharmacy.getId(), product.getId())
            .orElseGet(() -> inventoryRepository.save(buildNewInventory(
                pharmacy.getId(),
                product.getId(),
                product.getMedicine().getId(),
                request.getReorderLevel()
            )));

        int previousQuantity = batch.getQuantity() == null ? 0 : batch.getQuantity();
        int updatedQuantity = request.getQuantity() == null ? 0 : request.getQuantity();

        if (currentInventory.getId().equals(targetInventory.getId())) {
            int totalQuantity = currentInventory.getTotalQuantity() == null ? 0 : currentInventory.getTotalQuantity();
            int reservedQuantity = currentInventory.getReservedQuantity() == null ? 0 : currentInventory.getReservedQuantity();
            int newTotalQuantity = totalQuantity - previousQuantity + updatedQuantity;
            if (newTotalQuantity < reservedQuantity) {
                throw new InventoryValidationException("Cannot reduce batch quantity below reserved stock");
            }
            currentInventory.setTotalQuantity(newTotalQuantity);
            if (request.getReorderLevel() != null) {
                currentInventory.setReorderLevel(request.getReorderLevel());
            }
            inventoryRepository.save(currentInventory);
        } else {
            int oldTotal = currentInventory.getTotalQuantity() == null ? 0 : currentInventory.getTotalQuantity();
            int oldReserved = currentInventory.getReservedQuantity() == null ? 0 : currentInventory.getReservedQuantity();
            int newOldTotal = oldTotal - previousQuantity;
            if (newOldTotal < oldReserved) {
                throw new InventoryValidationException("Cannot move batch because reserved stock exceeds remaining stock");
            }
            currentInventory.setTotalQuantity(newOldTotal);
            inventoryRepository.save(currentInventory);

            int targetTotal = targetInventory.getTotalQuantity() == null ? 0 : targetInventory.getTotalQuantity();
            targetInventory.setTotalQuantity(targetTotal + updatedQuantity);
            if (request.getReorderLevel() != null) {
                targetInventory.setReorderLevel(request.getReorderLevel());
            }
            inventoryRepository.save(targetInventory);

            batch.setInventory(targetInventory);
        }

        batch.setBatchNumber(request.getBatchNumber());
        batch.setManufacturingDate(request.getManufacturingDate());
        batch.setExpiryDate(request.getExpiryDate());
        batch.setQuantity(updatedQuantity);
        batch.setUnitCost(request.getUnitCost());
        batch.setSellingPrice(request.getSellingPrice());
        batch.setStatus(BatchStatus.ACTIVE);

        Batch savedBatch = batchRepository.save(batch);
        return batchMapper.toResponse(savedBatch);
    }

    private com.TenaMed.pharmacy.entity.Pharmacy resolvePharmacyForActor(java.util.UUID actorUserId) {
        // Try owner first
        java.util.Optional<com.TenaMed.pharmacy.entity.Pharmacy> asOwner = pharmacyRepository.findByOwnerId(actorUserId);
        if (asOwner.isPresent()) {
            return asOwner.get();
        }

        // Try pharmacist
        java.util.List<com.TenaMed.pharmacy.entity.UserPharmacy> associations = userPharmacyRepository.findByUserId(actorUserId);
        if (associations.isEmpty()) {
            throw new InventoryValidationException("No pharmacy association found for user");
        }

        return associations.get(0).getPharmacy();
    }

    private Inventory buildNewInventory(java.util.UUID pharmacyId, java.util.UUID productId, java.util.UUID medicineId, Integer reorderLevel) {
        Inventory inventory = new Inventory();
        inventory.setPharmacyId(pharmacyId);
        inventory.setProductId(productId);
        inventory.setMedicineId(medicineId);
        inventory.setTotalQuantity(0);
        inventory.setReservedQuantity(0);
        inventory.setReorderLevel(reorderLevel != null ? reorderLevel : 10);
        return inventory;
    }

    private Product resolveOrCreateProduct(AddBatchRequest request) {
        if (request.getProductId() != null) {
            java.util.Optional<Product> byId = productRepository.findById(request.getProductId());
            if (byId.isPresent()) {
                return byId.get();
            }
        }

        String medicineName = normalize(request.getMedicineName());
        String brandName = normalize(request.getBrandName());
        String manufacturer = normalize(request.getManufacturer());
        if (medicineName == null || brandName == null || manufacturer == null) {
            throw new InventoryValidationException("medicineName, brandName and manufacturer are required when productId is missing or invalid");
        }

        Medicine medicine = resolveOrCreateMedicine(medicineName);

        return productRepository.findByBrandNameAndManufacturer(brandName, manufacturer)
            .orElseGet(() -> createProduct(medicine, brandName, manufacturer));
    }

    private Product createProduct(Medicine medicine, String brandName, String manufacturer) {
        Product product = new Product();
        product.setBrandName(brandName);
        product.setManufacturer(manufacturer);
        product.setMedicine(medicine);
        return productRepository.save(product);
    }

    private Medicine resolveOrCreateMedicine(String medicineName) {
        return medicineRepository.findByNameIgnoreCase(medicineName)
            .orElseThrow(() -> new InventoryValidationException("Medicine not found: " + medicineName));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventory(UUID pharmacyId, UUID productId) {
        Inventory inventory = inventoryRepository.findByPharmacyIdAndProductId(pharmacyId, productId)
            .orElseThrow(() -> new InventoryNotFoundException(pharmacyId, productId));

        List<BatchResponse> batches = batchRepository.findByInventoryIdOrderByExpiryDateAsc(inventory.getId())
            .stream()
            .map(batchMapper::toResponse)
            .toList();

        return inventoryMapper.toResponse(inventory, batches);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryListItemResponse> getCurrentUserInventoryList(UUID actorUserId) {
        Pharmacy pharmacy = resolvePharmacyForActor(actorUserId);
        List<Inventory> inventories = inventoryRepository.findByPharmacyId(pharmacy.getId());
        if (inventories.isEmpty()) {
            return List.of();
        }

        List<UUID> productIds = inventories.stream().map(Inventory::getProductId).distinct().toList();
        Map<UUID, Product> productById = productRepository.findAllById(productIds).stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<UUID> medicineIds = inventories.stream().map(Inventory::getMedicineId).distinct().toList();
        Map<UUID, Medicine> medicineById = medicineRepository.findAllById(medicineIds).stream()
            .collect(Collectors.toMap(Medicine::getId, Function.identity()));

        List<UUID> inventoryIds = inventories.stream().map(Inventory::getId).toList();
        Map<UUID, List<Batch>> batchesByInventoryId = batchRepository.findByInventoryIdIn(inventoryIds).stream()
            .collect(Collectors.groupingBy(batch -> batch.getInventory().getId()));

        return inventories.stream().map(inventory -> {
            Product product = productById.get(inventory.getProductId());
            Medicine medicine = medicineById.get(inventory.getMedicineId());
            List<InventoryListItemResponse.BatchPriceResponse> batchPrices = batchesByInventoryId
                .getOrDefault(inventory.getId(), List.of())
                .stream()
                .map(batch -> InventoryListItemResponse.BatchPriceResponse.builder()
                    .batchId(batch.getId())
                    .batchNumber(batch.getBatchNumber())
                    .unitPrice(batch.getUnitCost())
                    .sellingPrice(batch.getSellingPrice())
                    .build())
                .toList();

            int totalQuantity = inventory.getTotalQuantity() == null ? 0 : inventory.getTotalQuantity();
            int reservedQuantity = inventory.getReservedQuantity() == null ? 0 : inventory.getReservedQuantity();

            return InventoryListItemResponse.builder()
                .inventoryId(inventory.getId())
                .productId(inventory.getProductId())
                .imageUrl(product != null ? product.getImageUrl() : null)
                .medicineName(medicine != null ? medicine.getName() : null)
                .totalQuantity(totalQuantity)
                .batchPrices(batchPrices)
                .brand(product != null ? product.getBrandName() : null)
                .manufacturer(product != null ? product.getManufacturer() : null)
                .remainingQuantity(totalQuantity - reservedQuantity)
                .build();
        }).toList();
    }

    @Override
    public void deleteBatch(UUID batchId, UUID actorUserId) {
        if (batchId == null) {
            throw new InventoryValidationException("batchId is required");
        }

        Pharmacy pharmacy = resolvePharmacyForActor(actorUserId);
        Batch batch = batchRepository.findByIdAndInventoryPharmacyId(batchId, pharmacy.getId())
            .orElseThrow(() -> new BatchNotFoundException(batchId));

        Inventory inventory = batch.getInventory();
        int batchQuantity = batch.getQuantity() == null ? 0 : batch.getQuantity();
        int totalQuantity = inventory.getTotalQuantity() == null ? 0 : inventory.getTotalQuantity();
        int reservedQuantity = inventory.getReservedQuantity() == null ? 0 : inventory.getReservedQuantity();
        int newTotalQuantity = totalQuantity - batchQuantity;

        if (newTotalQuantity < reservedQuantity) {
            throw new InventoryValidationException("Cannot delete batch because reserved stock exceeds remaining total stock");
        }

        inventory.setTotalQuantity(newTotalQuantity);
        inventoryRepository.save(inventory);
        batchRepository.delete(batch);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkAvailability(UUID pharmacyId, UUID productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return false;
        }
        return inventoryRepository.findByPharmacyIdAndProductId(pharmacyId, productId)
            .map(inventory -> (inventory.getTotalQuantity() - inventory.getReservedQuantity()) >= quantity)
            .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkAvailability(UUID productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            return false;
        }
        return inventoryRepository.existsAvailableByProductId(productId, quantity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findPharmacyIdsWithAvailableProduct(UUID productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            return List.of();
        }
        return inventoryRepository.findPharmacyIdsWithAvailableProduct(productId, quantity);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal resolveUnitPrice(UUID productId) {
        if (productId == null) {
            throw new InventoryValidationException("productId must never be null in Inventory operations");
        }

        List<Inventory> inventories = inventoryRepository.findByProductIdIn(List.of(productId));
        BigDecimal resultPrice = null;

        for (Inventory inventory : inventories) {
            List<BigDecimal> activePrices = batchRepository.findByInventoryIdAndStatusOrderByExpiryDateAsc(inventory.getId(), BatchStatus.ACTIVE).stream()
                .filter(batch -> batch.getQuantity() != null && batch.getQuantity() > 0)
                .map(Batch::getSellingPrice)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

            if (activePrices.size() > 1) {
                org.slf4j.LoggerFactory.getLogger(InventoryServiceImpl.class).error("Price source lock violation: Multiple active prices detected for product {} in pharmacy {}: {}", productId, inventory.getPharmacyId(), activePrices);
                throw new InventoryValidationException("Price mismatch detected for product " + productId + " at pharmacy " + inventory.getPharmacyId());
            }

            if (!activePrices.isEmpty()) {
                BigDecimal pharmacyPrice = activePrices.get(0);
                if (resultPrice == null || pharmacyPrice.compareTo(resultPrice) < 0) {
                    resultPrice = pharmacyPrice;
                }
            }
        }

        return resultPrice;
    }

    @Override
    public boolean reserveStock(UUID pharmacyId, UUID productId, Integer quantity) {
        return reserveStock(pharmacyId, productId, quantity, null);
    }

    @Override
    public boolean reserveStock(UUID pharmacyId, UUID productId, Integer quantity, UUID referenceId) {
        if (quantity == null || quantity <= 0) {
            return false;
        }

        Inventory inventory = inventoryRepository.findWithLockByPharmacyIdAndProductId(pharmacyId, productId)
            .orElse(null);

        if (inventory == null) {
            org.slf4j.LoggerFactory.getLogger(InventoryServiceImpl.class).warn("Stock failure: Product {} not found in pharmacy {}", productId, pharmacyId);
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
                "productId", productId.toString(),
                "changes", Map.of("reservedQuantity", Map.of("old", oldReservedQuantity, "new", inventory.getReservedQuantity()))
            )
        );
        return true;
    }

    @Override
    public boolean confirmStock(UUID pharmacyId, UUID productId, Integer quantity, UUID referenceId) {
        if (quantity == null || quantity <= 0) {
            return false;
        }

        Inventory inventory = inventoryRepository.findWithLockByPharmacyIdAndProductId(pharmacyId, productId)
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
                "productId", productId.toString(),
                "changes", Map.of(
                    "reservedQuantity", Map.of("old", oldReservedQuantity, "new", inventory.getReservedQuantity()),
                    "totalQuantity", Map.of("old", oldTotalQuantity, "new", inventory.getTotalQuantity())
                )
            )
        );
        return true;
    }

    @Override
    public void releaseStock(UUID pharmacyId, UUID productId, Integer quantity) {
        releaseStock(pharmacyId, productId, quantity, null);
    }

    @Override
    public void releaseStock(UUID pharmacyId, UUID productId, Integer quantity, UUID referenceId) {
        if (quantity == null || quantity <= 0) {
            return;
        }

        Inventory inventory = inventoryRepository.findWithLockByPharmacyIdAndProductId(pharmacyId, productId)
            .orElseThrow(() -> new InventoryNotFoundException(pharmacyId, productId));

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
                "productId", productId.toString(),
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