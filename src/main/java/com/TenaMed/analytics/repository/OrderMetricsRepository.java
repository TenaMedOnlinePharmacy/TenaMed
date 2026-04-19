package com.TenaMed.analytics.repository;

import com.TenaMed.analytics.entity.OrderMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderMetricsRepository extends JpaRepository<OrderMetrics, UUID> {
}
