package com.TenaMed.analytics.service.impl;

import com.TenaMed.analytics.entity.ActorActivityMetrics;
import com.TenaMed.analytics.entity.OrderMetrics;
import com.TenaMed.analytics.entity.PrescriptionMetrics;
import com.TenaMed.analytics.repository.ActorActivityMetricsRepository;
import com.TenaMed.analytics.repository.OrderMetricsRepository;
import com.TenaMed.analytics.repository.PrescriptionMetricsRepository;
import com.TenaMed.analytics.service.MetricsProjectionService;
import com.TenaMed.verification.event.PrescriptionRejectedEvent;
import com.TenaMed.verification.event.PrescriptionVerifiedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class MetricsProjectionServiceImpl implements MetricsProjectionService {

    private static final UUID NIL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final OrderMetricsRepository orderMetricsRepository;
    private final PrescriptionMetricsRepository prescriptionMetricsRepository;
    private final ActorActivityMetricsRepository actorActivityMetricsRepository;

    public MetricsProjectionServiceImpl(OrderMetricsRepository orderMetricsRepository,
                                        PrescriptionMetricsRepository prescriptionMetricsRepository,
                                        ActorActivityMetricsRepository actorActivityMetricsRepository) {
        this.orderMetricsRepository = orderMetricsRepository;
        this.prescriptionMetricsRepository = prescriptionMetricsRepository;
        this.actorActivityMetricsRepository = actorActivityMetricsRepository;
    }

    @Override
    public void projectDomainEvent(String eventType, Object payload) {
        Map<String, Object> body = asStringMap(payload);
        String normalizedEventType = normalize(eventType, "UNKNOWN_EVENT");
        LocalDateTime eventTime = parseEventTime(body.get("timestamp"));
        String correlationId = normalize(asString(body.get("correlationId")), UUID.randomUUID().toString());

        String actorType = normalize(asString(body.get("actorType")), "SYSTEM");
        UUID actorId = asUuid(body.get("actorId"));
        upsertActorActivity(actorType, actorId, normalizedEventType, eventTime, correlationId);

        switch (normalizedEventType) {
            case "ORDER_CREATED" -> handleOrderCreated(body, eventTime, correlationId);
            case "ORDER_ACCEPTED", "ORDER_REJECTED", "ORDER_PAYMENT_UPDATED" ->
                    handleOrderUpdated(body, normalizedEventType, eventTime, correlationId);
            case "PAYMENT_WEBHOOK_PROCESSED" -> handlePaymentWebhook(body, eventTime, correlationId);
            case "PRESCRIPTION_UPLOADED" -> handlePrescriptionCreated(body, eventTime, correlationId);
            case "PRESCRIPTION_VERIFIED", "PRESCRIPTION_REJECTED" ->
                    handlePrescriptionStatusFromDomain(body, normalizedEventType, eventTime, correlationId);
            default -> {
                // Not a dashboard projection event.
            }
        }
    }

    @Override
    public void projectPrescriptionVerified(PrescriptionVerifiedEvent event) {
        if (event == null || event.getPrescriptionId() == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        PrescriptionMetrics metrics = getOrCreatePrescription(event.getPrescriptionId(), now);
        metrics.setStatus(normalize(event.getNewStatus(), "VERIFIED"));
        if (metrics.getVerifiedAt() == null) {
            metrics.setVerifiedAt(now);
        }
        if (event.getActorId() != null) {
            metrics.setVerifiedBy(event.getActorId());
        }
        metrics.setUpdatedAt(now);
        prescriptionMetricsRepository.save(metrics);

        upsertActorActivity(normalize(event.getActorType(), "SYSTEM"), event.getActorId(), "PRESCRIPTION_VERIFIED", now, null);
    }

    @Override
    public void projectPrescriptionRejected(PrescriptionRejectedEvent event) {
        if (event == null || event.getPrescriptionId() == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        PrescriptionMetrics metrics = getOrCreatePrescription(event.getPrescriptionId(), now);
        metrics.setStatus(normalize(event.getNewStatus(), "REJECTED"));
        metrics.setRejectionReason(event.getReason());
        if (metrics.getRejectedAt() == null) {
            metrics.setRejectedAt(now);
        }
        metrics.setUpdatedAt(now);
        prescriptionMetricsRepository.save(metrics);

        upsertActorActivity(normalize(event.getActorType(), "SYSTEM"), event.getActorId(), "PRESCRIPTION_REJECTED", now, null);
    }

    private void handleOrderCreated(Map<String, Object> payload, LocalDateTime eventTime, String correlationId) {
        UUID orderId = asUuid(payload.get("entityId"));
        if (orderId == null) {
            return;
        }

        if (orderMetricsRepository.existsById(orderId)) {
            return;
        }

        Map<String, Object> metadata = extractMetadata(payload);
        OrderMetrics row = new OrderMetrics();
        row.setOrderId(orderId);
        row.setPharmacyId(asUuid(payload.get("contextId")));
        row.setCustomerId(asUuid(payload.get("actorId")));
        row.setStatus(normalize(asString(metadata.get("status")), "PENDING_REVIEW"));
        row.setPaymentStatus(normalize(asString(metadata.get("paymentStatus")), "PENDING"));
        row.setAmount(asBigDecimal(metadata.get("amount")));
        row.setCreatedAt(eventTime);
        row.setUpdatedAt(eventTime);
        row.setCorrelationId(correlationId);
        orderMetricsRepository.save(row);
    }

    private void handleOrderUpdated(Map<String, Object> payload, String eventType, LocalDateTime eventTime, String correlationId) {
        UUID orderId = asUuid(payload.get("entityId"));
        if (orderId == null) {
            return;
        }

        Map<String, Object> metadata = extractMetadata(payload);
        Map<String, Object> changes = extractChanges(metadata);

        OrderMetrics row = getOrCreateOrder(orderId, eventTime);
        applyOrderChange(row, changes, "status");
        applyOrderChange(row, changes, "paymentStatus");

        if ("ORDER_ACCEPTED".equals(eventType) && row.getAcceptedAt() == null) {
            row.setAcceptedAt(eventTime);
        }
        if ("ORDER_REJECTED".equals(eventType) && row.getRejectedAt() == null) {
            row.setRejectedAt(eventTime);
        }
        if ("ORDER_PAYMENT_UPDATED".equals(eventType)
                && "SUCCESS".equalsIgnoreCase(row.getPaymentStatus())
                && row.getPaidAt() == null) {
            row.setPaidAt(eventTime);
        }

        row.setUpdatedAt(eventTime);
        row.setCorrelationId(correlationId);
        orderMetricsRepository.save(row);
    }

    private void handlePaymentWebhook(Map<String, Object> payload, LocalDateTime eventTime, String correlationId) {
        Map<String, Object> metadata = extractMetadata(payload);
        String result = normalize(asString(metadata.get("result")), "");
        if (!"SUCCESS".equalsIgnoreCase(result)) {
            return;
        }

        UUID orderId = asUuid(metadata.get("orderId"));
        if (orderId == null) {
            return;
        }

        OrderMetrics row = getOrCreateOrder(orderId, eventTime);
        if (row.getPaidAt() == null) {
            row.setPaidAt(eventTime);
        }
        row.setPaymentStatus("SUCCESS");
        row.setUpdatedAt(eventTime);
        row.setCorrelationId(correlationId);
        orderMetricsRepository.save(row);
    }

    private void handlePrescriptionCreated(Map<String, Object> payload, LocalDateTime eventTime, String correlationId) {
        UUID prescriptionId = asUuid(payload.get("entityId"));
        if (prescriptionId == null) {
            return;
        }

        if (prescriptionMetricsRepository.existsById(prescriptionId)) {
            return;
        }

        PrescriptionMetrics row = new PrescriptionMetrics();
        row.setPrescriptionId(prescriptionId);
        row.setStatus("UPLOADED");
        row.setCreatedAt(eventTime);
        row.setUpdatedAt(eventTime);
        row.setCorrelationId(correlationId);
        prescriptionMetricsRepository.save(row);
    }

    private void handlePrescriptionStatusFromDomain(Map<String, Object> payload,
                                                    String eventType,
                                                    LocalDateTime eventTime,
                                                    String correlationId) {
        UUID prescriptionId = asUuid(payload.get("entityId"));
        if (prescriptionId == null) {
            return;
        }

        Map<String, Object> metadata = extractMetadata(payload);
        Map<String, Object> changes = extractChanges(metadata);

        PrescriptionMetrics row = getOrCreatePrescription(prescriptionId, eventTime);
        Object statusChange = changes.get("status");
        if (statusChange instanceof Map<?, ?> changeMap) {
            String newStatus = asString(changeMap.get("new"));
            if (newStatus != null) {
                row.setStatus(newStatus);
            }
        }

        if ("PRESCRIPTION_VERIFIED".equals(eventType) && row.getVerifiedAt() == null) {
            row.setVerifiedAt(eventTime);
        }
        if ("PRESCRIPTION_REJECTED".equals(eventType) && row.getRejectedAt() == null) {
            row.setRejectedAt(eventTime);
            String reason = asString(metadata.get("reason"));
            if (reason != null) {
                row.setRejectionReason(reason);
            }
        }

        row.setUpdatedAt(eventTime);
        row.setCorrelationId(correlationId);
        prescriptionMetricsRepository.save(row);
    }

    private void upsertActorActivity(String actorType,
                                     UUID actorId,
                                     String eventType,
                                     LocalDateTime eventTime,
                                     String correlationId) {
        String normalizedActorType = normalize(actorType, "SYSTEM");
        UUID normalizedActorId = actorId == null ? NIL_UUID : actorId;
        LocalDate day = eventTime.toLocalDate();

        ActorActivityMetrics row = actorActivityMetricsRepository
                .findByActorTypeAndActorIdAndActivityDate(normalizedActorType, normalizedActorId, day)
                .orElseGet(() -> {
                    ActorActivityMetrics metrics = new ActorActivityMetrics();
                    metrics.setId(UUID.randomUUID());
                    metrics.setActorType(normalizedActorType);
                    metrics.setActorId(normalizedActorId);
                    metrics.setActivityDate(day);
                    metrics.setTotalEvents(0L);
                    metrics.setOrderEvents(0L);
                    metrics.setPrescriptionEvents(0L);
                    metrics.setPaymentEvents(0L);
                    metrics.setVerificationEvents(0L);
                    metrics.setUpdatedAt(eventTime);
                    return metrics;
                });

        if (correlationId != null
                && correlationId.equals(row.getLastEventCorrelationId())
                && eventType.equals(row.getLastEventType())) {
            return;
        }

        row.setTotalEvents(row.getTotalEvents() + 1);
        if (eventType.startsWith("ORDER_")) {
            row.setOrderEvents(row.getOrderEvents() + 1);
        }
        if (eventType.startsWith("PRESCRIPTION_")) {
            row.setPrescriptionEvents(row.getPrescriptionEvents() + 1);
        }
        if (eventType.startsWith("PAYMENT_")) {
            row.setPaymentEvents(row.getPaymentEvents() + 1);
        }
        if ("PRESCRIPTION_VERIFIED".equals(eventType) || "PRESCRIPTION_REJECTED".equals(eventType)) {
            row.setVerificationEvents(row.getVerificationEvents() + 1);
        }

        row.setLastEventType(eventType);
        row.setLastEventAt(eventTime);
        row.setLastEventCorrelationId(correlationId);
        row.setUpdatedAt(eventTime);
        actorActivityMetricsRepository.save(row);
    }

    private OrderMetrics getOrCreateOrder(UUID orderId, LocalDateTime eventTime) {
        return orderMetricsRepository.findById(orderId).orElseGet(() -> {
            OrderMetrics row = new OrderMetrics();
            row.setOrderId(orderId);
            row.setCreatedAt(eventTime);
            row.setUpdatedAt(eventTime);
            return row;
        });
    }

    private PrescriptionMetrics getOrCreatePrescription(UUID prescriptionId, LocalDateTime eventTime) {
        return prescriptionMetricsRepository.findById(prescriptionId).orElseGet(() -> {
            PrescriptionMetrics row = new PrescriptionMetrics();
            row.setPrescriptionId(prescriptionId);
            row.setCreatedAt(eventTime);
            row.setUpdatedAt(eventTime);
            return row;
        });
    }

    private void applyOrderChange(OrderMetrics row, Map<String, Object> changes, String fieldName) {
        Object rawChange = changes.get(fieldName);
        if (!(rawChange instanceof Map<?, ?> map)) {
            return;
        }

        String nextValue = asString(map.get("new"));
        if (nextValue == null) {
            return;
        }

        if ("status".equals(fieldName)) {
            row.setStatus(nextValue);
        }
        if ("paymentStatus".equals(fieldName)) {
            row.setPaymentStatus(nextValue);
        }
    }

    private Map<String, Object> extractMetadata(Map<String, Object> payload) {
        Object metadata = payload.get("metadata");
        return metadata instanceof Map<?, ?> map ? toStringObjectMap(map) : Map.of();
    }

    private Map<String, Object> extractChanges(Map<String, Object> metadata) {
        Object changes = metadata.get("changes");
        return changes instanceof Map<?, ?> map ? toStringObjectMap(map) : Map.of();
    }

    private Map<String, Object> toStringObjectMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    private UUID asUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String text) {
            try {
                return UUID.fromString(text.trim());
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private LocalDateTime parseEventTime(Object timestamp) {
        if (timestamp instanceof String text && !text.isBlank()) {
            try {
                return LocalDateTime.ofInstant(Instant.parse(text), ZoneOffset.UTC);
            } catch (Exception ignored) {
                // fallback below
            }
        }
        return LocalDateTime.now();
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
