package com.TenaMed.audit.service;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class AuditLogWriteRequest {

    private String entityType;
    private UUID entityId;
    private String action;
    private Map<String, Object> actionDetails;
    private Map<String, Object> changes;
    private String actorType;
    private UUID actorId;
    private String contextType;
    private UUID contextId;
    private String correlationId;
}
