package com.TenaMed.prescription.repository;

import com.TenaMed.prescription.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

        @Modifying
        @Query("""
            UPDATE Prescription p
            SET p.status = 'PROCESSING',
                p.rejectionReason = null
            WHERE p.id = :id
            """)
        int markProcessing(@Param("id") UUID id);

        @Modifying
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
}
