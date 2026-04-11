package com.TenaMed.pharmacy.service;

import com.TenaMed.pharmacy.dto.request.CreatePharmacyRequest;
import com.TenaMed.pharmacy.dto.response.PharmacyResponse;

import java.util.UUID;

public interface PharmacyService {

    PharmacyResponse createPharmacy(CreatePharmacyRequest request);

    PharmacyResponse verifyPharmacy(UUID pharmacyId, UUID verifiedBy);

    PharmacyResponse getPharmacy(UUID pharmacyId);
}