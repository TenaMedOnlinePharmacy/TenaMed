package com.TenaMed.analytics.service;

import com.TenaMed.verification.event.PrescriptionRejectedEvent;
import com.TenaMed.verification.event.PrescriptionVerifiedEvent;

import java.util.Map;

public interface MetricsProjectionService {

    void projectDomainEvent(String eventType, Object payload);

    void projectPrescriptionVerified(PrescriptionVerifiedEvent event);

    void projectPrescriptionRejected(PrescriptionRejectedEvent event);

    default Map<String, Object> asStringMap(Object payload) {
        return payload instanceof Map<?, ?> map ? map.entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .collect(java.util.stream.Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        Map.Entry::getValue,
                        (left, right) -> right,
                        java.util.LinkedHashMap::new
                )) : Map.of();
    }
}
