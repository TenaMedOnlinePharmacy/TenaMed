package com.TenaMed.medicine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "allergens")
@Getter
@Setter
@NoArgsConstructor
public class Allergen extends BaseAuditableEntity {

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "allergen_type")
    private String allergenType;

    @OneToMany(mappedBy = "allergen")
    private Set<MedicineAllergen> medicineAllergens = new LinkedHashSet<>();
}