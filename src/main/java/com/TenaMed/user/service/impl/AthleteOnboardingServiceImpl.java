package com.TenaMed.user.service.impl;

import com.TenaMed.antidoping.entity.AthleteProfile;
import com.TenaMed.antidoping.repository.AthleteProfileRepository;
import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.events.DomainEventService;
import com.TenaMed.user.dto.RegisterAthleteRequestDto;
import com.TenaMed.user.dto.RegisterAthleteResponseDto;
import com.TenaMed.user.dto.RegisterRequestDto;
import com.TenaMed.user.dto.RegisterResponseDto;
import com.TenaMed.user.service.AthleteOnboardingService;
import com.TenaMed.user.service.IdentityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Service
public class AthleteOnboardingServiceImpl implements AthleteOnboardingService {

    private static final String PATIENT_ROLE = "PATIENT";

    private final IdentityService identityService;
    private final AthleteProfileRepository athleteProfileRepository;
    private final DomainEventService domainEventService;

    public AthleteOnboardingServiceImpl(IdentityService identityService,
                                        AthleteProfileRepository athleteProfileRepository,
                                        DomainEventService domainEventService) {
        this.identityService = identityService;
        this.athleteProfileRepository = athleteProfileRepository;
        this.domainEventService = domainEventService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterAthleteResponseDto registerAthlete(RegisterAthleteRequestDto requestDto) {
        validateRequest(requestDto);

        identityService.populateRoles();

        RegisterRequestDto athleteRequest = new RegisterRequestDto();
        athleteRequest.setEmail(requestDto.getEmail());
        athleteRequest.setPassword(requestDto.getPassword());
        athleteRequest.setFirstName(requestDto.getFirstName());
        athleteRequest.setLastName(requestDto.getLastName());
        athleteRequest.setPhone(requestDto.getPhone());
        athleteRequest.setAddress(requestDto.getAddress());
        athleteRequest.setRoleNames(Set.of(PATIENT_ROLE));

        RegisterResponseDto athleteResponse = identityService.register(athleteRequest);

        AthleteProfile athleteProfile = new AthleteProfile();
        athleteProfile.setUserId(athleteResponse.getUserId());
        athleteProfile.setAdvisorEnabled(requestDto.getAdvisorEnabled() == null ? Boolean.TRUE : requestDto.getAdvisorEnabled());

        AthleteProfile savedProfile = athleteProfileRepository.save(athleteProfile);

        RegisterAthleteResponseDto.AthleteProfileDto athleteProfileDto =
                new RegisterAthleteResponseDto.AthleteProfileDto(
                        savedProfile.getUserId(),
                        savedProfile.getAdvisorEnabled(),
                        savedProfile.getCreatedAt()
                );

        domainEventService.publish(
            "ATHLETE_ONBOARDED",
            "ATHLETE_PROFILE",
            savedProfile.getUserId(),
            "ATHLETE",
            savedProfile.getUserId(),
            "PLATFORM",
            null,
            Map.of("advisorEnabled", Boolean.TRUE.equals(savedProfile.getAdvisorEnabled()))
        );

        return new RegisterAthleteResponseDto(athleteResponse, athleteProfileDto);
    }

    private void validateRequest(RegisterAthleteRequestDto requestDto) {
        if (requestDto == null) {
            throw new BadRequestException("Athlete payload is required");
        }
    }
}
