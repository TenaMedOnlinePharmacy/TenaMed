package com.TenaMed.user.service;

import com.TenaMed.antidoping.entity.AthleteProfile;
import com.TenaMed.antidoping.repository.AthleteProfileRepository;
import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.user.dto.RegisterAthleteRequestDto;
import com.TenaMed.user.dto.RegisterAthleteResponseDto;
import com.TenaMed.user.dto.RegisterRequestDto;
import com.TenaMed.user.dto.RegisterResponseDto;
import com.TenaMed.user.service.impl.AthleteOnboardingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AthleteOnboardingServiceImplTests {

    @Mock
    private IdentityService identityService;

    @Mock
    private AthleteProfileRepository athleteProfileRepository;

    @InjectMocks
    private AthleteOnboardingServiceImpl onboardingService;

    @Test
    void shouldRegisterAthleteWithPatientRoleAndCreateProfile() {
        UUID athleteUserId = UUID.randomUUID();

        RegisterAthleteRequestDto request = new RegisterAthleteRequestDto();
        request.setEmail("athlete@tenamed.com");
        request.setPassword("StrongPass123");
        request.setFirstName("Liya");
        request.setLastName("Bekele");
        request.setPhone("+251933333333");
        request.setAddress(Map.of("city", "Addis Ababa"));
        request.setAdvisorEnabled(false);

        RegisterResponseDto athleteResponse = RegisterResponseDto.builder()
                .userId(athleteUserId)
                .email("athlete@tenamed.com")
                .roles(List.of("PATIENT"))
                .build();

        AthleteProfile savedProfile = new AthleteProfile();
        savedProfile.setUserId(athleteUserId);
        savedProfile.setAdvisorEnabled(false);
        savedProfile.setCreatedAt(LocalDateTime.now());

        when(identityService.populateRoles()).thenReturn(List.of("PATIENT"));
        when(identityService.register(any(RegisterRequestDto.class))).thenReturn(athleteResponse);
        when(athleteProfileRepository.save(any(AthleteProfile.class))).thenReturn(savedProfile);

        RegisterAthleteResponseDto actual = onboardingService.registerAthlete(request);

        ArgumentCaptor<RegisterRequestDto> registerCaptor = ArgumentCaptor.forClass(RegisterRequestDto.class);
        verify(identityService).register(registerCaptor.capture());
        assertEquals("athlete@tenamed.com", registerCaptor.getValue().getEmail());
        assertEquals(Set.of("PATIENT"), registerCaptor.getValue().getRoleNames());

        ArgumentCaptor<AthleteProfile> profileCaptor = ArgumentCaptor.forClass(AthleteProfile.class);
        verify(athleteProfileRepository).save(profileCaptor.capture());
        assertEquals(athleteUserId, profileCaptor.getValue().getUserId());
        assertEquals(false, profileCaptor.getValue().getAdvisorEnabled());

        assertEquals(athleteUserId, actual.getAthlete().getUserId());
        assertEquals(false, actual.getAthleteProfile().getAdvisorEnabled());
    }

    @Test
    void shouldDefaultAdvisorEnabledToTrueWhenNotProvided() {
        UUID athleteUserId = UUID.randomUUID();

        RegisterAthleteRequestDto request = new RegisterAthleteRequestDto();
        request.setEmail("athlete@tenamed.com");
        request.setPassword("StrongPass123");
        request.setFirstName("Liya");
        request.setLastName("Bekele");
        request.setPhone("+251933333333");

        RegisterResponseDto athleteResponse = RegisterResponseDto.builder()
                .userId(athleteUserId)
                .email("athlete@tenamed.com")
                .roles(List.of("PATIENT"))
                .build();

        AthleteProfile savedProfile = new AthleteProfile();
        savedProfile.setUserId(athleteUserId);
        savedProfile.setAdvisorEnabled(true);

        when(identityService.populateRoles()).thenReturn(List.of("PATIENT"));
        when(identityService.register(any(RegisterRequestDto.class))).thenReturn(athleteResponse);
        when(athleteProfileRepository.save(any(AthleteProfile.class))).thenReturn(savedProfile);

        RegisterAthleteResponseDto actual = onboardingService.registerAthlete(request);

        ArgumentCaptor<AthleteProfile> profileCaptor = ArgumentCaptor.forClass(AthleteProfile.class);
        verify(athleteProfileRepository).save(profileCaptor.capture());
        assertEquals(true, profileCaptor.getValue().getAdvisorEnabled());
        assertEquals(true, actual.getAthleteProfile().getAdvisorEnabled());
    }

    @Test
    void shouldRejectWhenPayloadMissing() {
        assertThrows(BadRequestException.class, () -> onboardingService.registerAthlete(null));
    }
}
