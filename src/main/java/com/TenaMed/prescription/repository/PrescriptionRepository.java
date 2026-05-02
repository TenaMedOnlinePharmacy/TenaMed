package com.TenaMed.prescription.repository;

import com.TenaMed.prescription.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

    Optional<Prescription> findById(UUID id);

    java.util.List<Prescription> findByDoctorId(UUID doctorId);

    Optional<Prescription> findByIdAndDoctorId(UUID id, UUID doctorId);

    boolean existsByUniqueCode(String uniqueCode);

    @Modifying
    @Transactional
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

    @Modifying
    @Transactional
    @Query("""
            UPDATE Prescription p
            SET p.status = 'PROCESSING',
                p.rejectionReason = null
            WHERE p.id = :id
            """)
    int markProcessing(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("""
            UPDATE Prescription p
            SET p.status = 'FAILED',
                p.rejectionReason = :errorMessage,
                p.isVerified = false,
                p.verifiedBy = null,
                p.verifiedAt = null
            WHERE p.id = :id
            """)
    int markFailed(
            @Param("id") UUID id,
            @Param("errorMessage") String errorMessage
    );

    @Modifying
    @Transactional
    @Query("""
            UPDATE Prescription p
            SET p.status = 'VERIFIED',
            p.isVerified = true,
            p.verifiedBy = :verifiedBy,
            p.verifiedAt = :verifiedAt,
            p.reviewReason = null,
            p.rejectionReason = null
            WHERE p.id = :id
            """)
    int markVerified(
            @Param("id") UUID id,
            @Param("verifiedBy") UUID verifiedBy,
            @Param("verifiedAt") LocalDateTime verifiedAt
    );

    @Modifying
    @Transactional
    @Query("""
            UPDATE Prescription p
            SET p.status = 'VERIFIED',
            p.isVerified = true,
            p.verifiedBy = :verifiedBy,
            p.verifiedAt = :verifiedAt,
            p.rejectionReason = null
            WHERE p.id = :id
            """)
    int markVerifiedPreserveReviewReason(
            @Param("id") UUID id,
            @Param("verifiedBy") UUID verifiedBy,
            @Param("verifiedAt") LocalDateTime verifiedAt
    );

    @Modifying
    @Transactional
    @Query("""
            UPDATE Prescription p
            SET p.status = 'PENDING_MANUAL_REVIEW',
            p.reviewReason = :reviewReason,
            p.isVerified = false,
            p.verifiedBy = null,
            p.verifiedAt = null
            WHERE p.id = :id
            """)
    int markPendingManualReview(
            @Param("id") UUID id,
            @Param("reviewReason") String reviewReason
    );

    @Modifying
    @Transactional
    @Query("""
            UPDATE Prescription p
            SET p.status = 'REJECTED',
            p.rejectionReason = :rejectionReason,
            p.isVerified = false,
            p.verifiedBy = :verifiedBy,
            p.verifiedAt = null
            WHERE p.id = :id
            """)
    int markRejected(
            @Param("id") UUID id,
            @Param("rejectionReason") String rejectionReason,
            @Param("verifiedBy") UUID verifiedBy
    );

    @Modifying
    @Transactional
    @Query("""
            UPDATE Prescription p
            SET p.profileId = :profileId,
                p.patientId = NULL
            WHERE p.patientId = :patientId
            """)
    void migratePatientToProfile(
            @Param("patientId") UUID patientId,
            @Param("profileId") UUID profileId
    );

    Long countByHospitalId(UUID hospitalId);

    Optional<Object> findByUniqueCode(String uniqueCode);

    long countByOcrSuccessIsNotNull();

    long countByOcrSuccess(Boolean ocrSuccess);

    @Query("SELECT AVG(p.confidenceScore) FROM Prescription p WHERE p.confidenceScore IS NOT NULL AND p.ocrSuccess IS true ")
    Double getAverageOcrConfidence();

    @Query("SELECT p FROM Prescription p WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:highRisk IS NULL OR p.highRisk = :highRisk)")
    org.springframework.data.domain.Page<Prescription> findByStatusAndHighRisk(
            @Param("status") String status, 
            @Param("highRisk") Boolean highRisk, 
            org.springframework.data.domain.Pageable pageable);
}
