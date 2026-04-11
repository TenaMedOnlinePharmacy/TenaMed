package com.TenaMed.pharmacy.integration;

import com.TenaMed.payment.service.PaymentService;
import com.TenaMed.pharmacy.exception.ExternalModuleException;
import org.springframework.stereotype.Component;

@Component
public class PaymentAdapter {

    private final PaymentService paymentService;

    public PaymentAdapter(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public String verifyPayment(String txRef) {
        try {
            return paymentService.verifyPayment(txRef);
        } catch (Exception ex) {
            throw new ExternalModuleException("Failed to verify payment status", ex);
        }
    }
}