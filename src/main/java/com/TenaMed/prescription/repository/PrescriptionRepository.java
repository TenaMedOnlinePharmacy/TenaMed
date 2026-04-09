package com.TenaMed.prescription.repository;

import com.TenaMed.prescription.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

    Optional<Prescription> findById(UUID id);

    @Modifying
    @Query("""
            UPDATE Prescription p
            SET p.ocrSuccess = :ocrSuccess,
                p.confidenceScore = :confidenceScore
            WHERE p.id = :id
            """)
    int updateOcrOutcomeById(
            @Param("id") UUID id,
            @Param("ocrSuccess") Boolean ocrSuccess,
            @Param("confidenceScore") Double confidenceScore
    );
}
