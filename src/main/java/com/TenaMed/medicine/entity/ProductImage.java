package com.TenaMed.medicine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "product_images",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_product_images_product_pharmacy_primary",
            columnNames = {"product_id", "pharmacy_id", "is_primary"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ProductImage {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "pharmacy_id")
    private UUID pharmacyId;

    @Column(name = "image_url", nullable = false)
    @Size(max = 5000)
    private String imageUrl;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
