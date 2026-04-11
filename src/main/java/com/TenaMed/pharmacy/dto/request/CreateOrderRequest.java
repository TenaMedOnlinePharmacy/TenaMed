package com.TenaMed.pharmacy.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateOrderRequest {

    @NotNull
    private UUID customerId;

    @NotNull
    private UUID pharmacyId;

    private UUID prescriptionId;

    @Valid
    @NotEmpty
    private List<OrderItemRequest> items;
}