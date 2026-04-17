package com.TenaMed.antidoping.repository;

import com.TenaMed.antidoping.entity.RxNormMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RxNormMappingRepository extends JpaRepository<RxNormMapping, UUID> {

    Optional<RxNormMapping> findByMedicineNameIgnoreCase(String name);
}
