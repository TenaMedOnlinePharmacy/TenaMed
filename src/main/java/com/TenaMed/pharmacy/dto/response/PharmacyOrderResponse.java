package com.TenaMed.pharmacy.dto.response;

import com.TenaMed.pharmacy.enums.OrderStatus;
import com.TenaMed.prescription.entity.PrescriptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyOrderResponse {
    private UUID orderId;
    private OrderStatus status;
    private String prescriptionImage;
    private List<PharmacyOrderItemResponse> orderItems;
    private PrescriptionType type;
    private Boolean highRisk;
    private Double confidenceScore;
    private BigDecimal totalAmount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PharmacyOrderItemResponse {
        private String medicineName;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}
