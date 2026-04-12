package com.TenaMed.inventory.entity;

import com.TenaMed.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
    name = "inventory",
    uniqueConstraints = @UniqueConstraint(name = "uk_inventory_pharmacy_medicine", columnNames = {"pharmacy_id", "medicine_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class Inventory extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "pharmacy_id", nullable = false)
    private UUID pharmacyId;

    @Column(name = "medicine_id", nullable = false)
    private UUID medicineId;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Column(name = "reorder_level")
    private Integer reorderLevel;

    @OneToMany(mappedBy = "inventory", fetch = FetchType.LAZY)
    private Set<Batch> batches = new LinkedHashSet<>();
}