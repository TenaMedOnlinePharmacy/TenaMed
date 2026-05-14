package com.TenaMed.pharmacy.repository;

import com.TenaMed.pharmacy.entity.OrderItem;
import com.TenaMed.pharmacy.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

	boolean existsByOrderCustomerIdAndInventoryIdAndOrderStatusIn(UUID customerId,
																  UUID inventoryId,
																  Collection<OrderStatus> statuses);

	Optional<OrderItem> findTopByOrderCustomerIdAndInventoryIdAndOrderStatusInOrderByOrderCreatedAtDesc(UUID customerId,
																										UUID inventoryId,
																										Collection<OrderStatus> statuses);
}