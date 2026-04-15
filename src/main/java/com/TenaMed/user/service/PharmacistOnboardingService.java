package com.TenaMed.user.service;

import com.TenaMed.user.dto.RegisterPharmacistRequestDto;
import com.TenaMed.user.dto.RegisterPharmacistResponseDto;

public interface PharmacistOnboardingService {

    RegisterPharmacistResponseDto registerPharmacist(RegisterPharmacistRequestDto requestDto);
}