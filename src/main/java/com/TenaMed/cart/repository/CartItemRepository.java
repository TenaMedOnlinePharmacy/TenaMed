package com.TenaMed.cart.repository;

import com.TenaMed.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

	Optional<CartItem> findByCartIdAndProductIdAndPharmacyId(UUID cartId, UUID productId, UUID pharmacyId);
}
