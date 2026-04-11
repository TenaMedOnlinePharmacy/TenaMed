package com.TenaMed.pharmacy.service;

import com.TenaMed.pharmacy.dto.request.OrderItemRequest;

import java.util.List;
import java.util.UUID;

public interface PharmacyInventoryValidator {

    boolean itemsBelongToPharmacy(UUID pharmacyId, List<OrderItemRequest> items);
}