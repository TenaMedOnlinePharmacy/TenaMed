package com.TenaMed.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookResponse {
    private String message;
    private String status;
    private UUID txRef;
    private UUID paymentId;
    private UUID orderId;
    private String orderStatus;
    private String orderPaymentStatus;
}
