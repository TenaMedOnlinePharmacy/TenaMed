package com.TenaMed.pharmacy.dto.request;

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
    private UUID pharmacyId;

    private UUID prescriptionId;

    @NotEmpty
    private List<Item> items;

    @Getter
    @Setter
    public static class Item {
        @NotNull
        private UUID prescriptionItemId;

        @NotNull
        private UUID productId;
    }

}