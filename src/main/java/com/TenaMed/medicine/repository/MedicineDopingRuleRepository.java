package com.TenaMed.medicine.repository;

import com.TenaMed.medicine.entity.MedicineDopingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MedicineDopingRuleRepository extends JpaRepository<MedicineDopingRule, UUID> {

    Optional<MedicineDopingRule> findByIdAndMedicine_Id(UUID id, UUID medicineId);
}