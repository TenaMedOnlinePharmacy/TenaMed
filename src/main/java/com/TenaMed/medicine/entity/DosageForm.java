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
@Table(name = "dosage_forms")
@Getter
@Setter
@NoArgsConstructor
public class DosageForm extends BaseCreatedAtEntity {

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 50)
    private String abbreviation;

    @OneToMany(mappedBy = "dosageForm")
    private Set<Medicine> medicines = new LinkedHashSet<>();
}
