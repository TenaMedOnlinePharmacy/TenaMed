package com.TenaMed.medicine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "medicine_doping_rules")
@Getter
@Setter
@NoArgsConstructor
public class MedicineDopingRule extends BaseUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @Column(length = 100)
    private String ruleset;

    private Integer rulesetYear;

    @Column(length = 100)
    private String status;

    @Column(length = 2000)
    private String notes;
}
