package com.TenaMed.pharmacy.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ProductRatingSummaryResponse {

    private UUID inventoryId;
    private Double averageRating;
    private Integer ratingCount;
    private List<ProductRatingResponse> ratings;
}
