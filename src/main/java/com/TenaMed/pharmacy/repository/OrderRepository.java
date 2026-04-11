package com.TenaMed.pharmacy.repository;

import com.TenaMed.pharmacy.entity.Order;
import com.TenaMed.pharmacy.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByCustomerId(UUID customerId);

    List<Order> findByPharmacyId(UUID pharmacyId);

    List<Order> findByStatus(OrderStatus status);
}