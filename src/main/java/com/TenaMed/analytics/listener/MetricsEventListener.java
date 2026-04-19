package com.TenaMed.analytics.listener;

import com.TenaMed.analytics.service.MetricsProjectionService;
import com.TenaMed.manualreview.event.DomainEvent;
import com.TenaMed.verification.event.PrescriptionRejectedEvent;
import com.TenaMed.verification.event.PrescriptionVerifiedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MetricsEventListener {

    private final MetricsProjectionService metricsProjectionService;

    public MetricsEventListener(MetricsProjectionService metricsProjectionService) {
        this.metricsProjectionService = metricsProjectionService;
    }

    @EventListener
    public void onDomainEvent(DomainEvent event) {
        if (event == null) {
            return;
        }
        metricsProjectionService.projectDomainEvent(event.getEventType(), event.getPayload());
    }

    @EventListener
    public void onPrescriptionVerified(PrescriptionVerifiedEvent event) {
        if (event == null) {
            return;
        }
        metricsProjectionService.projectPrescriptionVerified(event);
    }

    @EventListener
    public void onPrescriptionRejected(PrescriptionRejectedEvent event) {
        if (event == null) {
            return;
        }
        metricsProjectionService.projectPrescriptionRejected(event);
    }
}
