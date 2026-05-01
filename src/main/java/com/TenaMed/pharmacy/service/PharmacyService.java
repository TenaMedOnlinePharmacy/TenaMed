package com.TenaMed.pharmacy.service;

import com.TenaMed.pharmacy.dto.request.CreatePharmacyRequest;
import com.TenaMed.pharmacy.dto.response.PharmacyResponse;
import com.TenaMed.invitation.dto.InvitationResponseDto;

import java.util.UUID;

public interface PharmacyService {

    PharmacyResponse createPharmacy(CreatePharmacyRequest request);

    PharmacyResponse verifyPharmacy(UUID pharmacyId);

    PharmacyResponse rejectPharmacy(UUID pharmacyId);

    PharmacyResponse suspendPharmacy(UUID pharmacyId);

    PharmacyResponse unsuspendPharmacy(UUID pharmacyId);

    PharmacyResponse getPharmacy(UUID pharmacyId);

    InvitationResponseDto invitePharmacist(UUID pharmacyId, String email);
}