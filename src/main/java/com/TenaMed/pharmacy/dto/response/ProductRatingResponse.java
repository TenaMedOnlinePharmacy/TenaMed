package com.TenaMed.pharmacy.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ProductRatingResponse {

    private UUID id;
    private UUID inventoryId;
    private UUID userId;
    private UUID orderId;
    private Integer rating;
    private String reviewText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
