package com.TenaMed.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCartItemQuantityRequest {

    @NotNull
    @Min(1)
    private Integer quantity;
}
