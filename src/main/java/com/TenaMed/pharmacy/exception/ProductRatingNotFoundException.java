package com.TenaMed.pharmacy.exception;

import java.util.UUID;

public class ProductRatingNotFoundException extends PharmacyException {

    public ProductRatingNotFoundException(UUID ratingId) {
        super("Product rating not found: " + ratingId);
    }
}
