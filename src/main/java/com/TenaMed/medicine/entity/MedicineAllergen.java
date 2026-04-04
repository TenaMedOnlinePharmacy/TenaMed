package com.TenaMed.medicine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "medicine_allergens",
        uniqueConstraints = @UniqueConstraint(name = "uk_medicine_allergen", columnNames = {"medicine_id", "allergen_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class MedicineAllergen extends BaseCreatedAtEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "allergen_id", nullable = false)
    private Allergen allergen;
}
