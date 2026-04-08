package com.TenaMed.prescription.repository;

import com.TenaMed.prescription.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

    Optional<Prescription> findById(UUID id);
}
