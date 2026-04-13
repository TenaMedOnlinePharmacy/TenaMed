package com.TenaMed.manualreview.event;

public interface DomainEventPublisher {

    void publish(String eventType, Object payload);
}
