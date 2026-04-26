package com.TenaMed.pharmacy.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
public class PrescriptionProductOptionDto {
    private UUID productId;
    private String brandName;
    private BigDecimal price;
    private boolean available;
    private Integer availableQuantity;
    private UUID pharmacyId;
    private String pharmacyName;
}
