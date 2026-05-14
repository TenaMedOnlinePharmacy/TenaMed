package com.TenaMed.pharmacy.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductRatingUpsertResponse {

    private ProductRatingResponse rating;
    private Double averageRating;
    private Integer ratingCount;
}
