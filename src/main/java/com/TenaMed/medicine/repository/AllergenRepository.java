package com.TenaMed.medicine.repository;

import com.TenaMed.medicine.entity.Allergen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AllergenRepository extends JpaRepository<Allergen, UUID> {
    Optional<Allergen> findByNameIgnoreCase(String name);
}