package com.TenaMed.user.service;

import com.TenaMed.user.dto.RegisterHospitalOwnerRequestDto;
import com.TenaMed.user.dto.RegisterHospitalOwnerResponseDto;

public interface HospitalOwnerOnboardingService {

    RegisterHospitalOwnerResponseDto registerHospitalOwner(RegisterHospitalOwnerRequestDto requestDto);
}
