package com.TenaMed.pharmacy.service;

import com.TenaMed.pharmacy.dto.request.ProductRatingRequest;
import com.TenaMed.pharmacy.dto.response.ProductRatingSummaryResponse;
import com.TenaMed.pharmacy.dto.response.ProductRatingUpsertResponse;

import java.util.UUID;

public interface ProductRatingService {

    ProductRatingUpsertResponse createOrUpdateRating(UUID customerId, ProductRatingRequest request);

    ProductRatingSummaryResponse getRatingsForInventory(UUID inventoryId);

    void deleteRating(UUID ratingId, UUID actorUserId, boolean isAdmin);
}
