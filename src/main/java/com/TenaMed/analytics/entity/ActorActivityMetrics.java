package com.TenaMed.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "actor_activity_metrics",
        uniqueConstraints = @UniqueConstraint(name = "uk_actor_activity_day", columnNames = {"actor_type", "actor_id", "activity_date"})
)
@Getter
@Setter
@NoArgsConstructor
public class ActorActivityMetrics {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "actor_type", nullable = false, length = 60)
    private String actorType;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "total_events", nullable = false)
    private Long totalEvents;

    @Column(name = "order_events", nullable = false)
    private Long orderEvents;

    @Column(name = "prescription_events", nullable = false)
    private Long prescriptionEvents;

    @Column(name = "payment_events", nullable = false)
    private Long paymentEvents;

    @Column(name = "verification_events", nullable = false)
    private Long verificationEvents;

    @Column(name = "last_event_type", length = 120)
    private String lastEventType;

    @Column(name = "last_event_at")
    private LocalDateTime lastEventAt;

    @Column(name = "last_event_correlation_id", length = 100)
    private String lastEventCorrelationId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
