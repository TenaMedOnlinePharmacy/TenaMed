package com.TenaMed.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelPaymentResponse {
    private String message;
    private String status;
    private CancelPaymentData data;
}
