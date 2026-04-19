package com.TenaMed.analytics.repository;

import com.TenaMed.analytics.entity.PrescriptionMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrescriptionMetricsRepository extends JpaRepository<PrescriptionMetrics, UUID> {
}
