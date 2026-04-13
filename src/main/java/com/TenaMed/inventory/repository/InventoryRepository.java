package com.TenaMed.inventory.repository;

import com.TenaMed.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    List<Inventory> findByMedicineIdIn(Collection<UUID> medicineIds);

    Optional<Inventory> findByPharmacyIdAndMedicineId(UUID pharmacyId, UUID medicineId);

        @Query("""
                     select case when count(i) > 0 then true else false end
                     from Inventory i
                     where i.medicineId = :medicineId
                         and (i.totalQuantity - i.reservedQuantity) >= :quantity
                     """)
        boolean existsAvailableByMedicineId(UUID medicineId, Integer quantity);

        @Query("""
                     select i.pharmacyId
                     from Inventory i
                     where i.medicineId = :medicineId
                         and (i.totalQuantity - i.reservedQuantity) >= :quantity
                     """)
        List<UUID> findPharmacyIdsWithAvailableMedicine(UUID medicineId, Integer quantity);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findWithLockByPharmacyIdAndMedicineId(UUID pharmacyId, UUID medicineId);
}