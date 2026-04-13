package com.TenaMed.pharmacy.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateOrderFromCartRequest {

    @NotEmpty
    private List<Item> items;

    @Getter
    @Setter
    public static class Item {
        @NotNull
        private UUID medicineId;

        @NotNull
        @Min(1)
        private Integer quantity;

        @NotNull
        private BigDecimal unitPrice;

        private UUID prescriptionId;
    }
}
