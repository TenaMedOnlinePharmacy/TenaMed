package com.TenaMed.delivery.repository;

import com.TenaMed.delivery.entity.Delivery;
import com.TenaMed.delivery.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    List<Delivery> findByStatus(DeliveryStatus status);

    Optional<Delivery> findByOrderId(UUID orderId);
}
