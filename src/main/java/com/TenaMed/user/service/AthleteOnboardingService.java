package com.TenaMed.user.service;

import com.TenaMed.user.dto.RegisterAthleteRequestDto;
import com.TenaMed.user.dto.RegisterAthleteResponseDto;

public interface AthleteOnboardingService {

    RegisterAthleteResponseDto registerAthlete(RegisterAthleteRequestDto requestDto);
}
