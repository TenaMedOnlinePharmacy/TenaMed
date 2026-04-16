package com.TenaMed.pharmacy.service;

import com.TenaMed.pharmacy.dto.request.PharmacistInviteRegistrationRequestDto;
import com.TenaMed.pharmacy.dto.response.StaffResponse;

public interface PharmacistOnboardingService {

    StaffResponse registerAndCreatePharmacistFromInvite(String token, PharmacistInviteRegistrationRequestDto requestDto);
}
