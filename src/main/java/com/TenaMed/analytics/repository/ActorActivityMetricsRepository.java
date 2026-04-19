package com.TenaMed.analytics.repository;

import com.TenaMed.analytics.entity.ActorActivityMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ActorActivityMetricsRepository extends JpaRepository<ActorActivityMetrics, UUID> {

    Optional<ActorActivityMetrics> findByActorTypeAndActorIdAndActivityDate(String actorType, UUID actorId, LocalDate activityDate);
}
