package com.TenaMed.antidoping.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity(name = "AntidopingMedicineDopingRule")
@Table(
    name = "medicine_doping_rules",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_medicine_doping_rules_medicine_ruleset_year",
            columnNames = {"medicine_id", "ruleset", "ruleset_year"}
        )
    },
    indexes = {
        @Index(name = "idx_medicine_doping_rules_medicine_id", columnList = "medicine_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class MedicineDopingRule {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "medicine_id", nullable = false)
    private UUID medicineId;

    @Column(name = "ruleset", nullable = false, length = 100)
    private String ruleset;

    @Column(name = "ruleset_year", nullable = false)
    private Integer rulesetYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MedicineDopingRuleStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "checked_at", nullable = false, updatable = false)
    private LocalDateTime checkedAt;
}
