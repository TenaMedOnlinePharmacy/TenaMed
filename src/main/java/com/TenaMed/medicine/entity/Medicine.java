package com.TenaMed.medicine.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "medicines")
@Getter
@Setter
@NoArgsConstructor
public class Medicine extends BaseAuditableEntity {

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(name = "generic_name")
    private String genericName;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dosage_form_id", nullable = false)
    private DosageForm dosageForm;

    @Column(name = "therapeutic_class")
    private String therapeuticClass;

    private String schedule;

    @Column(name = "need_manual_review", nullable = false)
    private boolean needManualReview;

    @Column(name = "dose_value")
    private BigDecimal doseValue;

    @Column(name = "dose_unit")
    private String doseUnit;

    @Column(name = "regulatory_code", unique = true)
    private String regulatoryCode;

    @Column(name = "requires_prescription")
    private boolean requiresPrescription;

    @Column(columnDefinition = "TEXT")
    private String indications;

    @Column(columnDefinition = "TEXT")
    private String contraindications;

    @Column(name = "side_effects", columnDefinition = "TEXT")
    private String sideEffects;

    @Column(name = "dosage_instructions", columnDefinition = "TEXT")
    private String dosageInstructions;

    @Column(name = "pregnancy_category", length = 1)
    private String pregnancyCategory;

    @Column(name = "image_url")
    private String imageUrl;

    @OneToMany(mappedBy = "medicine", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MedicineAllergen> medicineAllergens = new LinkedHashSet<>();
}
