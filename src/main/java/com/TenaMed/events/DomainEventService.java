package com.TenaMed.events;

import java.util.Map;
import java.util.UUID;

public interface DomainEventService {

    void publish(String eventType,
                 String entityType,
                 UUID entityId,
                 String contextType,
                 UUID contextId,
                 Map<String, Object> metadata);

    void publish(String eventType,
                 String entityType,
                 UUID entityId,
                 String actorType,
                 UUID actorId,
                 String contextType,
                 UUID contextId,
                 Map<String, Object> metadata);
}
