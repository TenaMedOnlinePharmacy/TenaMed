package com.TenaMed.payment.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class PaymentRequest {
    private UUID orderId;
}
