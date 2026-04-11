package com.TenaMed.inventory.repository;

import com.TenaMed.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    List<Inventory> findByMedicineIdIn(Collection<UUID> medicineIds);

    Optional<Inventory> findByPharmacyIdAndMedicineId(UUID pharmacyId, UUID medicineId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findWithLockByPharmacyIdAndMedicineId(UUID pharmacyId, UUID medicineId);
}