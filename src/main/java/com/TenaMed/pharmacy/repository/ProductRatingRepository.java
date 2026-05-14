package com.TenaMed.pharmacy.repository;

import com.TenaMed.pharmacy.entity.ProductRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRatingRepository extends JpaRepository<ProductRating, UUID> {

    Optional<ProductRating> findByUserIdAndInventoryId(UUID userId, UUID inventoryId);

    List<ProductRating> findByInventoryIdOrderByCreatedAtDesc(UUID inventoryId);

    @Query("select coalesce(avg(r.rating), 0), count(r) from ProductRating r where r.inventory.id = :inventoryId")
    Object[] getAggregateByInventoryId(@Param("inventoryId") UUID inventoryId);
}
