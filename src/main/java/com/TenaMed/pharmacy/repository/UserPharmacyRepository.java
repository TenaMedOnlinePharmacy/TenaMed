package com.TenaMed.pharmacy.repository;

import com.TenaMed.pharmacy.entity.UserPharmacy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserPharmacyRepository extends JpaRepository<UserPharmacy, UUID> {

    List<UserPharmacy> findByUserId(UUID userId);

    List<UserPharmacy> findByPharmacyId(UUID pharmacyId);

    boolean existsByUserIdAndPharmacyId(UUID userId, UUID pharmacyId);
}