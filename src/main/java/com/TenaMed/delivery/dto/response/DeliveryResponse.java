package com.TenaMed.delivery.dto.response;

import com.TenaMed.delivery.enums.DeliveryStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DeliveryResponse {

    private UUID id;
    private UUID orderId;
    private DeliveryStatus status;
    private String deliveryAddress;
    private LocalDateTime dispatchedAt;
    private LocalDateTime deliveredAt;
    private String failureReason;
    private LocalDateTime createdAt;
    private String customerPhone;
}
