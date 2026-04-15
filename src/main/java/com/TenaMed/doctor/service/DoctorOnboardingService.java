package com.TenaMed.doctor.service;

import com.TenaMed.doctor.dto.DoctorInviteRegistrationRequestDto;
import com.TenaMed.doctor.dto.DoctorResponseDto;

public interface DoctorOnboardingService {

    DoctorResponseDto registerAndCreateDoctorFromInvite(String token, DoctorInviteRegistrationRequestDto requestDto);
}