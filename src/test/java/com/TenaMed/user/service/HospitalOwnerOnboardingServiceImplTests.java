package com.TenaMed.user.service;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.hospital.dto.HospitalRequestDto;
import com.TenaMed.hospital.dto.HospitalResponseDto;
import com.TenaMed.hospital.service.HospitalService;
import com.TenaMed.ocr.service.SupabaseStorageService;
import com.TenaMed.user.dto.RegisterHospitalOwnerRequestDto;
import com.TenaMed.user.dto.RegisterHospitalOwnerResponseDto;
import com.TenaMed.user.dto.RegisterRequestDto;
import com.TenaMed.user.dto.RegisterResponseDto;
import com.TenaMed.user.service.impl.HospitalOwnerOnboardingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HospitalOwnerOnboardingServiceImplTests {

    @Mock
    private IdentityService identityService;

    @Mock
    private HospitalService hospitalService;

    @Mock
    private SupabaseStorageService supabaseStorageService;

    @InjectMocks
    private HospitalOwnerOnboardingServiceImpl onboardingService;

    @Test
    void shouldRegisterOwnerUploadLicenseAndCreateHospital() {
        UUID ownerUserId = UUID.randomUUID();

        RegisterHospitalOwnerRequestDto request = new RegisterHospitalOwnerRequestDto();
        RegisterHospitalOwnerRequestDto.OwnerDto owner = new RegisterHospitalOwnerRequestDto.OwnerDto();
        owner.setEmail("owner@tenamed.com");
        owner.setPassword("StrongPass123");
        owner.setFirstName("Abel");
        owner.setLastName("Kassa");
        owner.setPhone("+251900000000");
        request.setOwner(owner);

        RegisterHospitalOwnerRequestDto.HospitalDto hospital = new RegisterHospitalOwnerRequestDto.HospitalDto();
        hospital.setName("Central Hospital");
        hospital.setLicenseNumber("HOS-1001");
        hospital.setLicenseImage(new MockMultipartFile("license", "license.png", "image/png", "img".getBytes()));
        request.setHospital(hospital);

        RegisterResponseDto ownerResponse = RegisterResponseDto.builder()
                .userId(ownerUserId)
                .email("owner@tenamed.com")
            .roles(List.of("HOSPITAL_OWNER"))
                .build();

        HospitalResponseDto hospitalResponse = new HospitalResponseDto();
        hospitalResponse.setOwnerId(ownerUserId);
        hospitalResponse.setName("Central Hospital");

        when(identityService.populateRoles()).thenReturn(List.of("HOSPITAL_OWNER"));
        when(identityService.register(any(RegisterRequestDto.class))).thenReturn(ownerResponse);
        when(supabaseStorageService.uploadAndGetSignedUrl(any())).thenReturn("https://supabase.example/license.png");
        when(hospitalService.createHospitalForOwner(any(HospitalRequestDto.class), eq(ownerUserId))).thenReturn(hospitalResponse);

        RegisterHospitalOwnerResponseDto actual = onboardingService.registerHospitalOwner(request);

        ArgumentCaptor<RegisterRequestDto> ownerCaptor = ArgumentCaptor.forClass(RegisterRequestDto.class);
        verify(identityService).register(ownerCaptor.capture());
        assertEquals("owner@tenamed.com", ownerCaptor.getValue().getEmail());
        assertEquals(Set.of("HOSPITAL_OWNER"), ownerCaptor.getValue().getRoleNames());

        ArgumentCaptor<HospitalRequestDto> hospitalCaptor = ArgumentCaptor.forClass(HospitalRequestDto.class);
        verify(hospitalService).createHospitalForOwner(hospitalCaptor.capture(), eq(ownerUserId));
        assertEquals("https://supabase.example/license.png", hospitalCaptor.getValue().getLicenseImageUrl());
        assertEquals("Central Hospital", actual.getHospital().getName());
        assertEquals(ownerUserId, actual.getOwner().getUserId());
    }

    @Test
    void shouldRejectWhenLicenseImageMissing() {
        RegisterHospitalOwnerRequestDto request = new RegisterHospitalOwnerRequestDto();
        request.setOwner(new RegisterHospitalOwnerRequestDto.OwnerDto());
        request.setHospital(new RegisterHospitalOwnerRequestDto.HospitalDto());

        assertThrows(BadRequestException.class, () -> onboardingService.registerHospitalOwner(request));
    }
}
