package com.TenaMed.audit.service.impl;

import com.TenaMed.audit.entity.AuditLog;
import com.TenaMed.audit.repository.AuditLogRepository;
import com.TenaMed.audit.service.AuditLogService;
import com.TenaMed.audit.service.AuditLogWriteRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

    private static final String DEFAULT_ENTITY_TYPE = "UNKNOWN";
    private static final String DEFAULT_ACTION = "UNKNOWN_EVENT";
    private static final String DEFAULT_ACTOR_TYPE = "SYSTEM";
    private static final String DEFAULT_CONTEXT_TYPE = "PLATFORM";

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public AuditLog write(AuditLogWriteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        String correlationId = normalize(request.getCorrelationId());
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        AuditLog log = new AuditLog(
                UUID.randomUUID(),
                normalizedOrDefault(request.getEntityType(), DEFAULT_ENTITY_TYPE),
                request.getEntityId(),
                normalizedOrDefault(request.getAction(), DEFAULT_ACTION),
                mapOrEmpty(request.getActionDetails()),
                mapOrEmpty(request.getChanges()),
                normalizedOrDefault(request.getActorType(), DEFAULT_ACTOR_TYPE),
                request.getActorId(),
                normalizedOrDefault(request.getContextType(), DEFAULT_CONTEXT_TYPE),
                request.getContextId(),
                correlationId
        );

        return auditLogRepository.save(log);
    }

    private Map<String, Object> mapOrEmpty(Map<String, Object> value) {
        return value == null ? Map.of() : value;
    }

    private String normalizedOrDefault(String value, String fallback) {
        String normalized = normalize(value);
        return normalized == null ? fallback : normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
