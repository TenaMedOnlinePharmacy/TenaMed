package com.TenaMed.inventory.service;

import com.TenaMed.inventory.dto.AddBatchRequest;
import com.TenaMed.inventory.dto.BatchResponse;
import com.TenaMed.inventory.dto.CreateInventoryRequest;
import com.TenaMed.inventory.dto.InventoryListItemResponse;
import com.TenaMed.inventory.dto.InventoryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface InventoryService {

    InventoryResponse createInventory(CreateInventoryRequest request);

    BatchResponse addBatch(AddBatchRequest request, UUID actorUserId, MultipartFile image);

    InventoryResponse getInventory(UUID pharmacyId, UUID productId);

    List<InventoryListItemResponse> getCurrentUserInventoryList(UUID actorUserId);

    void deleteBatch(UUID batchId, UUID actorUserId);

    boolean checkAvailability(UUID pharmacyId, UUID productId, Integer quantity);

    boolean checkAvailability(UUID productId, Integer quantity);

    List<UUID> findPharmacyIdsWithAvailableProduct(UUID productId, Integer quantity);

    BigDecimal resolveUnitPrice(UUID productId);

    boolean reserveStock(UUID pharmacyId, UUID productId, Integer quantity);

    boolean reserveStock(UUID pharmacyId, UUID productId, Integer quantity, UUID referenceId);

    boolean confirmStock(UUID pharmacyId, UUID productId, Integer quantity, UUID referenceId);

    void releaseStock(UUID pharmacyId, UUID productId, Integer quantity);

    void releaseStock(UUID pharmacyId, UUID productId, Integer quantity, UUID referenceId);
}