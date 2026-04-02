package com.TenaMed.payment;

import lombok.Data;

@Data
public class PaymentRequest {
    private String amount;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
}
