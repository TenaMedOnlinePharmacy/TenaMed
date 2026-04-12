package com.TenaMed.payment.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class PaymentRequest {
    private UUID orderId;
    private String amount;
    private String paymentMethod;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
}
