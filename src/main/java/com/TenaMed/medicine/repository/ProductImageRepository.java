package com.TenaMed.medicine.repository;

import com.TenaMed.medicine.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    Optional<ProductImage> findByProductIdAndPharmacyIdAndIsPrimaryTrue(UUID productId, UUID pharmacyId);

    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.id = :productId AND pi.pharmacyId IS NULL AND pi.isPrimary = true")
    Optional<ProductImage> findDefaultByProductId(@Param("productId") UUID productId);

    List<ProductImage> findByProductIdOrderByPharmacyIdAscCreatedAtDesc(UUID productId);

    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.id IN :productIds AND pi.pharmacyId IS NULL AND pi.isPrimary = true")
    List<ProductImage> findDefaultsByProductIds(@Param("productIds") Collection<UUID> productIds);

    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.id IN :productIds AND pi.pharmacyId = :pharmacyId AND pi.isPrimary = true")
    List<ProductImage> findByProductIdsAndPharmacyId(@Param("productIds") Collection<UUID> productIds, @Param("pharmacyId") UUID pharmacyId);

    void deleteByProductIdAndPharmacyId(UUID productId, UUID pharmacyId);

    Optional<ProductImage> findFirstByProductIdAndIsPrimaryFalse(UUID productId);

    ProductImage findByProductIdAndPharmacyId(UUID id, UUID productId);
}
