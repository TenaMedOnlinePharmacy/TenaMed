# Pharmacy Product Rating API

## Overview
Ratings are tied to pharmacy inventory items (not the global medicine). Only PATIENT users can create or update ratings after completing a purchase.

## Endpoints

### 1) Create or Update Rating
POST /api/pharmacy/ratings

Request body:
```
{
  "inventoryId": "UUID",
  "rating": 5,
  "reviewText": "Very good service"
}
```

Response body:
```
{
  "rating": {
    "id": "UUID",
    "inventoryId": "UUID",
    "userId": "UUID",
    "orderId": "UUID",
    "rating": 5,
    "reviewText": "Very good service",
    "createdAt": "2026-05-14T12:34:56",
    "updatedAt": "2026-05-14T12:34:56"
  },
  "averageRating": 4.5,
  "ratingCount": 12
}
```

Notes:
- This endpoint is used on the product detail page to submit a new rating or update an existing one.
- It can also be used from the My Orders page to rate an item from a completed order.

### 2) Get Ratings for Inventory Item
GET /api/pharmacy/ratings/inventory/{inventoryId}

Response body:
```
{
  "inventoryId": "UUID",
  "averageRating": 4.5,
  "ratingCount": 12,
  "ratings": [
    {
      "id": "UUID",
      "inventoryId": "UUID",
      "userId": "UUID",
      "orderId": "UUID",
      "rating": 5,
      "reviewText": "Very good service",
      "createdAt": "2026-05-14T12:34:56",
      "updatedAt": "2026-05-14T12:34:56"
    }
  ]
}
```

### 3) Delete Rating (Optional)
DELETE /api/pharmacy/ratings/{ratingId}

Response: 204 No Content

Only the rating owner or an admin can delete.

## Response Integration
The following existing responses now include rating aggregates:
- Medicine search results
- OCR prescription matched products
- Pharmacy inventory list responses

Fields added:
```
"averageRating": 4.5,
"ratingCount": 12
```

## Validation and Exceptions

- 401 Unauthorized: user not authenticated.
- 403 Forbidden: deleting a rating you do not own (unless admin).
- 400 Bad Request:
  - rating out of range (must be 1 to 5)
  - inventory not found
  - user not found
  - rating a product without a completed purchase
- 404 Not Found: rating not found (delete)

## UI Notes
- Product detail page: allow patients to rate and review a product.
- My Orders page: allow patients to rate and review items from completed orders.
