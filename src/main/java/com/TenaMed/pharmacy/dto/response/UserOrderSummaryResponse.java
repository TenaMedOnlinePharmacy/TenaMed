package com.TenaMed.pharmacy.dto.response;

import com.TenaMed.pharmacy.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class UserOrderSummaryResponse {

    private UUID orderId;
    private List<String> productNames;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private LocalDateTime date;
}
