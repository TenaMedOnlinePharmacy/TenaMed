package com.TenaMed.pharmacy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectOrderRequest {

    @NotBlank
    private String rejectionReason;
}