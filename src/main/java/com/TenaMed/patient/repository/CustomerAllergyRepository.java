package com.TenaMed.patient.repository;

import com.TenaMed.patient.entity.CustomerAllergy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerAllergyRepository extends JpaRepository<CustomerAllergy, UUID> {

    boolean existsByProfile_IdAndAllergen_Id(UUID profileId, UUID allergenId);

    Optional<CustomerAllergy> findByIdAndProfile_UserId(UUID allergyId, UUID userId);

    List<CustomerAllergy> findByProfile_Id(UUID profileId);
}
