package com.TenaMed.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryFailRequest {

    @NotBlank
    private String reason;
}
