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

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("payload", event.getPayload());

        UUID prescriptionId = extractPrescriptionId(event.getPayload());

        auditLogService.write(AuditLogWriteRequest.builder()
                .entityType(prescriptionId == null ? "DOMAIN_EVENT" : "PRESCRIPTION")
                .entityId(prescriptionId)
                .action(normalizeAction(event.getEventType()))
                .actionDetails(payload)
                .changes(Map.of())
                .actorType(resolveActorType())
                .actorId(resolveActorId())
                .contextType(CONTEXT_PLATFORM)
                .contextId(null)
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
                .changes(Map.of("status", Map.of("to", "VERIFIED")))
                .actorType(resolveActorType())
                .actorId(resolveActorId())
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
                .changes(Map.of("status", Map.of("to", "REJECTED")))
                .actorType(resolveActorType())
                .actorId(resolveActorId())
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

    private UUID extractPrescriptionId(Object payload) {
        if (!(payload instanceof Map<?, ?> map)) {
            return null;
        }

        Object candidate = map.get("prescriptionId");
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
