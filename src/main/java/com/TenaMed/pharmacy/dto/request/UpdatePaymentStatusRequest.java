package com.TenaMed.pharmacy.dto.request;

import com.TenaMed.pharmacy.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePaymentStatusRequest {

    @NotNull
    private PaymentStatus paymentStatus;
}