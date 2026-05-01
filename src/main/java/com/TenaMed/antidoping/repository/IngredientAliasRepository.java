package com.TenaMed.antidoping.repository;

import com.TenaMed.antidoping.entity.IngredientAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IngredientAliasRepository extends JpaRepository<IngredientAlias, UUID> {
    Optional<IngredientAlias> findByNormalizedAlias(String normalizedAlias);
}
