package com.TenaMed.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AddCartItemRequest {

    @NotBlank
    private String medicineName;

    @NotBlank
    private String pharmacyName;

    @NotNull
    @Min(1)
    private Integer quantity;

    private UUID prescriptionId;
}
