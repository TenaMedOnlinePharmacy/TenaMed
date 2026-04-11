package com.TenaMed.pharmacy.service.impl;

import com.TenaMed.pharmacy.dto.request.OrderItemRequest;
import com.TenaMed.pharmacy.service.PharmacyInventoryValidator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class NoOpPharmacyInventoryValidator implements PharmacyInventoryValidator {

    @Override
    public boolean itemsBelongToPharmacy(UUID pharmacyId, List<OrderItemRequest> items) {
        return true;
    }
}