package com.TenaMed.patient.repository;

import com.TenaMed.patient.entity.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, UUID> {
    Optional<PatientProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    boolean existsByUniqueCode(String uniqueCode);

    boolean existsByUniqueCodeAndUserIdNot(String uniqueCode, UUID userId);
}
