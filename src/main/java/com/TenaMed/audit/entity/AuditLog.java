package com.TenaMed.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "entity_type", nullable = false, updatable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", updatable = false)
    private UUID entityId;

    @Column(name = "action", nullable = false, updatable = false, length = 120)
    private String action;

    @Column(name = "action_details", nullable = false, updatable = false, columnDefinition = "jsonb")
    private String actionDetails;

    @Column(name = "changes", nullable = false, updatable = false, columnDefinition = "jsonb")
    private String changes;

    @Column(name = "actor_type", nullable = false, updatable = false, length = 60)
    private String actorType;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "context_type", nullable = false, updatable = false, length = 60)
    private String contextType;

    @Column(name = "context_id", updatable = false)
    private UUID contextId;

    @Column(name = "correlation_id", nullable = false, updatable = false, length = 100)
    private String correlationId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AuditLog(UUID id,
                    String entityType,
                    UUID entityId,
                    String action,
                    String actionDetails,
                    String changes,
                    String actorType,
                    UUID actorId,
                    String contextType,
                    UUID contextId,
                    String correlationId) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.actionDetails = actionDetails;
        this.changes = changes;
        this.actorType = actorType;
        this.actorId = actorId;
        this.contextType = contextType;
        this.contextId = contextId;
        this.correlationId = correlationId;
    }

    @PreUpdate
    protected void onUpdate() {
        throw new UnsupportedOperationException("AuditLog is append-only and cannot be updated");
    }
}
