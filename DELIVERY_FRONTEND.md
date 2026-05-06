# Delivery Frontend Integration

## Overview
This document describes the Delivery API endpoints and response shapes to help implement frontend screens for delivery management.

## Roles and Access
- Only users with ROLE_PHARMACIST can call:
  - POST /api/deliveries/{id}/dispatch
  - POST /api/deliveries/{id}/deliver
  - POST /api/deliveries/{id}/fail
- Listing endpoints are accessible without role checks in the backend controller.

## Endpoints
### List deliveries by status
- GET /api/deliveries?status=READY_FOR_DELIVERY
  - Includes customer phone number in response.
- GET /api/deliveries?status=OUT_FOR_DELIVERY
- GET /api/deliveries?status=DELIVERED
- GET /api/deliveries?status=FAILED

### List failed deliveries
- GET /api/deliveries/failed

### Dispatch a delivery
- POST /api/deliveries/{id}/dispatch

### Mark delivered
- POST /api/deliveries/{id}/deliver

### Mark failed
- POST /api/deliveries/{id}/fail
- Body:
  {
    "reason": "..."
  }

## Delivery Response Shape
The list endpoints return a list of items with the following fields:
- id: string (UUID)
- orderId: string (UUID)
- status: READY_FOR_DELIVERY | OUT_FOR_DELIVERY | DELIVERED | FAILED
- deliveryAddress: string
- dispatchedAt: string | null (ISO timestamp)
- deliveredAt: string | null (ISO timestamp)
- failureReason: string | null
- createdAt: string (ISO timestamp)
- customerPhone: string | null

## UI Suggestions
- Delivery list tabs by status: Ready, Out for Delivery, Delivered, Failed.
- For READY_FOR_DELIVERY, display customer phone and delivery address prominently.
- For FAILED, show failureReason and allow re-dispatch flow if needed.
- For OUT_FOR_DELIVERY, allow only "Mark Delivered" and "Mark Failed" actions.

## Notes
- Delivery is created only when an order becomes CONFIRMED.
- Do not send latitude/longitude or distance fields (not supported).
