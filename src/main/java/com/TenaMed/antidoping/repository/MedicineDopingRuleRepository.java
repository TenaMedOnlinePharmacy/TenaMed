package com.TenaMed.antidoping.repository;

import com.TenaMed.antidoping.entity.MedicineDopingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MedicineDopingRuleRepository extends JpaRepository<MedicineDopingRule, UUID> {

    Optional<MedicineDopingRule> findByMedicineIdAndRulesetAndRulesetYear(UUID medicineId, String ruleset, Integer rulesetYear);
}
