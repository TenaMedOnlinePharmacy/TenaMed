package com.TenaMed.antidoping.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "rxnorm_mappings",
    indexes = {
        @Index(name = "idx_rxnorm_mappings_medicine_name", columnList = "medicine_name")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class RxNormMapping {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "medicine_name", nullable = false, length = 255)
    private String medicineName;

    @Column(name = "rxcui", nullable = false, length = 50)
    private String rxcui;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
