package com.TenaMed.manualreview.event;

import lombok.Value;

@Value
public class DomainEvent {
    String eventType;
    Object payload;
}
