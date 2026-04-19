package com.TenaMed.audit.listener;

import com.TenaMed.audit.service.AuditLogService;
import com.TenaMed.audit.service.AuditLogWriteRequest;
import com.TenaMed.manualreview.event.DomainEvent;
import com.TenaMed.ocr.event.PrescriptionPipelinePersistedEvent;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import com.TenaMed.verification.event.PrescriptionRejectedEvent;
import com.TenaMed.verification.event.PrescriptionVerifiedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class AuditEventListener {

    private static final String CONTEXT_PLATFORM = "PLATFORM";

    private final AuditLogService auditLogService;

    public AuditEventListener(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @EventListener
    public void onDomainEvent(DomainEvent event) {
        if (event == null) {
            return;
        }

        String entityType = extractString(event.getPayload(), "entityType", "DOMAIN_EVENT");
        UUID entityId = extractUuid(event.getPayload(), "entityId");
        String actorType = extractString(event.getPayload(), "actorType", resolveActorType());
        UUID actorId = extractUuid(event.getPayload(), "actorId");
        String contextType = extractString(event.getPayload(), "contextType", CONTEXT_PLATFORM);
        UUID contextId = extractUuid(event.getPayload(), "contextId");
        Map<String, Object> metadata = extractMetadata(event.getPayload());
        Map<String, Object> changes = extractChanges(metadata);
        Map<String, Object> actionDetails = stripChanges(metadata);

        auditLogService.write(AuditLogWriteRequest.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(normalizeAction(event.getEventType()))
                .actionDetails(actionDetails)
                .changes(changes)
                .actorType(actorType)
                .actorId(actorId == null ? resolveActorId() : actorId)
                .contextType(contextType)
                .contextId(contextId)
                .correlationId(resolveCorrelationId(event.getPayload()))
                .build());
    }

    @EventListener
    public void onPrescriptionVerified(PrescriptionVerifiedEvent event) {
        if (event == null) {
            return;
        }

        auditLogService.write(AuditLogWriteRequest.builder()
                .entityType("PRESCRIPTION")
                .entityId(event.getPrescriptionId())
                .action("PRESCRIPTION_VERIFIED")
                .actionDetails(Map.of("eventClass", PrescriptionVerifiedEvent.class.getSimpleName()))
            .changes(Map.of("status", Map.of("old", event.getOldStatus(), "new", event.getNewStatus())))
            .actorType(event.getActorType() == null ? resolveActorType() : event.getActorType())
            .actorId(event.getActorId() == null ? resolveActorId() : event.getActorId())
                .contextType(CONTEXT_PLATFORM)
                .contextId(null)
                .correlationId(null)
                .build());
    }

    @EventListener
    public void onPrescriptionRejected(PrescriptionRejectedEvent event) {
        if (event == null) {
            return;
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("eventClass", PrescriptionRejectedEvent.class.getSimpleName());
        details.put("reason", event.getReason());

        auditLogService.write(AuditLogWriteRequest.builder()
                .entityType("PRESCRIPTION")
                .entityId(event.getPrescriptionId())
                .action("PRESCRIPTION_REJECTED")
                .actionDetails(details)
                .changes(Map.of("status", Map.of("old", event.getOldStatus(), "new", event.getNewStatus())))
                .actorType(event.getActorType() == null ? resolveActorType() : event.getActorType())
                .actorId(event.getActorId() == null ? resolveActorId() : event.getActorId())
                .contextType(CONTEXT_PLATFORM)
                .contextId(null)
                .correlationId(null)
                .build());
    }

    @EventListener
    public void onPipelinePersisted(PrescriptionPipelinePersistedEvent event) {
        if (event == null) {
            return;
        }

        auditLogService.write(AuditLogWriteRequest.builder()
                .entityType("PRESCRIPTION")
                .entityId(event.prescriptionId())
                .action("OCR_PROCESSED")
                .actionDetails(Map.of(
                        "eventClass", PrescriptionPipelinePersistedEvent.class.getSimpleName(),
                        "medicinesCount", event.medicinesCount()
                ))
                .changes(Map.of())
                .actorType("SYSTEM")
                .actorId(null)
                .contextType(CONTEXT_PLATFORM)
                .contextId(null)
                .correlationId(null)
                .build());
    }

    private String normalizeAction(String value) {
        if (value == null || value.isBlank()) {
            return "DOMAIN_EVENT";
        }
        return value.trim().toUpperCase();
    }

    private UUID extractUuid(Object payload, String key) {
        if (!(payload instanceof Map<?, ?> map)) {
            return null;
        }

        Object candidate = map.get(key);
        if (candidate == null) {
            return null;
        }

        if (candidate instanceof UUID uuid) {
            return uuid;
        }

        if (candidate instanceof String text) {
            try {
                return UUID.fromString(text);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        return null;
    }

    private String extractString(Object payload, String key, String fallback) {
        if (!(payload instanceof Map<?, ?> map)) {
            return fallback;
        }
        Object value = map.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return text.trim();
        }
        return fallback;
    }

    private Map<String, Object> extractMetadata(Object payload) {
        if (!(payload instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Object metadata = map.get("metadata");
        if (metadata instanceof Map<?, ?> raw) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                if (entry.getKey() != null) {
                    result.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            return result;
        }
        return Map.of();
    }

    private Map<String, Object> extractChanges(Map<String, Object> metadata) {
        Object rawChanges = metadata.get("changes");
        if (rawChanges instanceof Map<?, ?> raw) {
            Map<String, Object> changes = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                if (entry.getKey() != null) {
                    changes.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            return changes;
        }
        return Map.of();
    }

    private Map<String, Object> stripChanges(Map<String, Object> metadata) {
        if (metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> details = new LinkedHashMap<>(metadata);
        details.remove("changes");
        return details;
    }

    private String resolveCorrelationId(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            Object candidate = map.get("correlationId");
            if (candidate instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }
        return null;
    }

    private String resolveActorType() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "SYSTEM";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUserPrincipal) {
            return "USER";
        }
        return "SYSTEM";
    }

    private UUID resolveActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUserPrincipal userPrincipal) {
            return userPrincipal.getUserId();
        }
        return null;
    }
}
