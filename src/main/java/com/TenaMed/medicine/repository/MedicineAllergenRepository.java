package com.TenaMed.medicine.repository;

import com.TenaMed.medicine.entity.MedicineAllergen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MedicineAllergenRepository extends JpaRepository<MedicineAllergen, UUID> {

    boolean existsByMedicine_IdAndAllergen_Id(UUID medicineId, UUID allergenId);

    Optional<MedicineAllergen> findByMedicine_IdAndAllergen_Id(UUID medicineId, UUID allergenId);
}