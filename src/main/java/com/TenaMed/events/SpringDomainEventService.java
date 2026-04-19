package com.TenaMed.events;

import com.TenaMed.manualreview.event.DomainEvent;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SpringDomainEventService implements DomainEventService {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringDomainEventService(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(String eventType,
                        String entityType,
                        UUID entityId,
                        String contextType,
                        UUID contextId,
                        Map<String, Object> metadata) {
        ActorResolution actor = resolveActor();
        publish(eventType, entityType, entityId, actor.actorType(), actor.actorId(), contextType, contextId, metadata);
    }

    @Override
    public void publish(String eventType,
                        String entityType,
                        UUID entityId,
                        String actorType,
                        UUID actorId,
                        String contextType,
                        UUID contextId,
                        Map<String, Object> metadata) {
        Map<String, Object> effectiveMetadata = metadata == null ? Map.of() : metadata;
        String correlationId = extractCorrelationId(effectiveMetadata);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", normalize(eventType, "UNKNOWN_EVENT"));
        payload.put("entityType", normalize(entityType, "UNKNOWN"));
        payload.put("entityId", entityId);
        payload.put("actorType", normalize(actorType, "SYSTEM"));
        payload.put("actorId", actorId);
        payload.put("contextType", normalize(contextType, "PLATFORM"));
        payload.put("contextId", contextId);
        payload.put("metadata", effectiveMetadata);
        payload.put("correlationId", correlationId);
        payload.put("timestamp", Instant.now().toString());

        applicationEventPublisher.publishEvent(new DomainEvent(normalize(eventType, "UNKNOWN_EVENT"), payload));
    }

    private ActorResolution resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return new ActorResolution("SYSTEM", null);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUserPrincipal userPrincipal) {
            return new ActorResolution("USER", userPrincipal.getUserId());
        }

        return new ActorResolution("SYSTEM", null);
    }

    private String extractCorrelationId(Map<String, Object> metadata) {
        Object existing = metadata.get("correlationId");
        if (existing instanceof String text && !text.isBlank()) {
            return text.trim();
        }
        return UUID.randomUUID().toString();
    }

    private String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private record ActorResolution(String actorType, UUID actorId) {
    }
}
