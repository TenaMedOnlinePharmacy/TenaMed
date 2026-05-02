package com.TenaMed.medicine.repository;

import com.TenaMed.medicine.entity.MedicineAllergen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface MedicineAllergenRepository extends JpaRepository<MedicineAllergen, UUID> {

    List<MedicineAllergen> findByMedicine_Id(UUID medicineId);

    boolean existsByMedicine_IdAndAllergen_Id(UUID medicineId, UUID allergenId);

    Optional<MedicineAllergen> findByMedicine_IdAndAllergen_Id(UUID medicineId, UUID allergenId);
}