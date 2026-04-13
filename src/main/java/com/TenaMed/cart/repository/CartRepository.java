package com.TenaMed.cart.repository;

import com.TenaMed.cart.entity.Cart;
import com.TenaMed.cart.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByUserIdAndStatus(UUID userId, CartStatus status);

    @Query("""
           select distinct c
           from Cart c
           left join fetch c.items
           where c.userId = :userId
             and c.status = :status
           """)
    Optional<Cart> findWithItemsByUserIdAndStatus(UUID userId, CartStatus status);
}
