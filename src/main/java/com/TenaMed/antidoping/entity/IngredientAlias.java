package com.TenaMed.antidoping.entity;

import com.TenaMed.antidoping.util.TextNormalizer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
    name = "ingredient_alias",
    indexes = {
        @Index(name = "idx_ingredient_alias_normalized", columnList = "normalized_alias"),
        @Index(name = "idx_ingredient_canonical_normalized", columnList = "normalized_canonical")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class IngredientAlias {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "alias_name", nullable = false, length = 255)
    private String aliasName;

    @Column(name = "normalized_alias", nullable = false, length = 255)
    private String normalizedAlias;

    @Column(name = "canonical_name", nullable = false, length = 255)
    private String canonicalName;

    @Column(name = "normalized_canonical", nullable = false, length = 255)
    private String normalizedCanonical;

    @PrePersist
    @PreUpdate
    public void prePersistOrUpdate() {
        if (this.aliasName != null) {
            this.normalizedAlias = TextNormalizer.normalize(this.aliasName);
        }
        if (this.canonicalName != null) {
            this.normalizedCanonical = TextNormalizer.normalize(this.canonicalName);
        }
    }
}
