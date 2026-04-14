package com.TenaMed.prescription.entity;

import com.TenaMed.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
public class Prescription extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "patient_id", nullable = true)
    private UUID patientId;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "original_images")
    @Size(max = 5000)
    private String originalImages;

    @Column(name = "ocr_response")
    private String ocrResponse;

    @Column(name = "is_verified")
    private Boolean isVerified;

    @Column(name = "max_refills_allowed")
    private Integer maxRefillsAllowed;

    @Column(name = "refills_used")
    private Integer refillsUsed;

    @Column(name = "hospital_id")
    private UUID hospitalId;

    @Column(name = "doctor_id")
    private UUID doctorId;

    @Column(name = "unique_code", unique = true)
    private String uniqueCode;

    @Column(name = "is_used")
    private Boolean isUsed;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "used_by")
    private UUID usedBy;


    @Column(name = "status")
    private String status;

    @Column(name = "review_reason", nullable = true)
    private String reviewReason;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "ocr_success")
    private Boolean ocrSuccess;

    @Column(name = "high_risk")
    private Boolean highRisk;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = true)
    private PrescriptionType type;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
}
