package com.TenaMed.antidoping.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
    name = "banned_substances",
    indexes = {
        @Index(name = "idx_banned_substances_ingredient_name", columnList = "ingredient_name")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class BannedSubstance {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ingredient_name", nullable = false, length = 255)
    private String ingredientName;

    @Column(name = "category", length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BannedSubstanceStatus status;

    @Column(name = "ruleset", nullable = false, length = 100)
    private String ruleset;

    @Column(name = "ruleset_year", nullable = false)
    private Integer rulesetYear;
}
