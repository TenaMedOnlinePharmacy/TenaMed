package com.TenaMed.antidoping.repository;

import com.TenaMed.antidoping.entity.BannedSubstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BannedSubstanceRepository extends JpaRepository<BannedSubstance, UUID> {

    List<BannedSubstance> findByIngredientNameInIgnoreCase(List<String> ingredients);

    List<BannedSubstance> findByNormalizedName(String normalizedName);

    @org.springframework.data.jpa.repository.Query("""
    SELECT b FROM BannedSubstance b
    WHERE b.normalizedName LIKE CONCAT('%', :ingredient, '%')
    """)
    List<BannedSubstance> findByNormalizedNameLike(String ingredient);
}
