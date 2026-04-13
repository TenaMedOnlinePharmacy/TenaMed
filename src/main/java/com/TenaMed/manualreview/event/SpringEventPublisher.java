package com.TenaMed.manualreview.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(String eventType, Object payload) {
        applicationEventPublisher.publishEvent(new DomainEvent(eventType, payload));
    }
}
