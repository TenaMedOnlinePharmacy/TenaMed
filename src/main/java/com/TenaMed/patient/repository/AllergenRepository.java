package com.TenaMed.patient.repository;

import com.TenaMed.patient.entity.Allergen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AllergenRepository extends JpaRepository<Allergen, UUID> {
    Optional<Allergen> findByNameIgnoreCase(String name);
}
