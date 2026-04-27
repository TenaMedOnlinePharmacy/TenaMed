package com.TenaMed.inventory.repository;

import com.TenaMed.inventory.entity.Batch;
import com.TenaMed.inventory.enums.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BatchRepository extends JpaRepository<Batch, UUID> {

    java.util.Optional<Batch> findByIdAndInventoryPharmacyId(UUID id, UUID pharmacyId);

    List<Batch> findByInventoryIdOrderByExpiryDateAsc(UUID inventoryId);

    List<Batch> findByInventoryIdAndStatusOrderByExpiryDateAsc(UUID inventoryId, BatchStatus status);

    List<Batch> findByInventoryIdIn(Iterable<UUID> inventoryIds);

    List<Batch> findByInventoryIdInAndStatus(Iterable<UUID> inventoryIds, BatchStatus status);
}