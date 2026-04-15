package com.TenaMed.user.service;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.ocr.service.SupabaseStorageService;
import com.TenaMed.pharmacy.dto.request.CreatePharmacyRequest;
import com.TenaMed.pharmacy.dto.response.PharmacyResponse;
import com.TenaMed.pharmacy.service.PharmacyService;
import com.TenaMed.user.dto.RegisterPharmacistRequestDto;
import com.TenaMed.user.dto.RegisterPharmacistResponseDto;
import com.TenaMed.user.dto.RegisterRequestDto;
import com.TenaMed.user.dto.RegisterResponseDto;
import com.TenaMed.user.service.impl.PharmacistOnboardingServiceImpl;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PharmacistOnboardingServiceImplTests {

    @Mock
    private IdentityService identityService;

    @Mock
    private PharmacyService pharmacyService;

    @Mock
    private SupabaseStorageService supabaseStorageService;

    @InjectMocks
    private PharmacistOnboardingServiceImpl onboardingService;

    @Test
    void shouldRegisterPharmacistUploadLicenseAndCreatePharmacy() {
        UUID pharmacistUserId = UUID.randomUUID();

        RegisterPharmacistRequestDto request = new RegisterPharmacistRequestDto();

        RegisterPharmacistRequestDto.OwnerDto pharmacist = new RegisterPharmacistRequestDto.OwnerDto();
        pharmacist.setEmail("pharmacist@tenamed.com");
        pharmacist.setPassword("StrongPass123");
        pharmacist.setFirstName("Sara");
        pharmacist.setLastName("Abebe");
        pharmacist.setPhone("+251911111111");
        request.setPharmacist(pharmacist);

        RegisterPharmacistRequestDto.PharmacyDto pharmacy = new RegisterPharmacistRequestDto.PharmacyDto();
        pharmacy.setName("City Pharmacy");
        pharmacy.setLegalName("City Pharmacy PLC");
        pharmacy.setLicenseNumber("PHR-2026-001");
        pharmacy.setEmail("city@pharmacy.com");
        pharmacy.setPhone("+251922222222");
        pharmacy.setLicenseImage(new MockMultipartFile("license", "license.png", "image/png", "img".getBytes()));
        request.setPharmacy(pharmacy);

        RegisterResponseDto pharmacistResponse = RegisterResponseDto.builder()
                .userId(pharmacistUserId)
                .email("pharmacist@tenamed.com")
                .roles(List.of("PHARMACIST"))
                .build();

        PharmacyResponse pharmacyResponse = PharmacyResponse.builder()
                .ownerId(pharmacistUserId)
                .name("City Pharmacy")
                .build();

        when(identityService.populateRoles()).thenReturn(List.of("PHARMACIST"));
        when(identityService.register(any(RegisterRequestDto.class))).thenReturn(pharmacistResponse);
        when(supabaseStorageService.uploadAndGetSignedUrl(any())).thenReturn("https://supabase.example/license.png");
        when(pharmacyService.createPharmacy(any(CreatePharmacyRequest.class))).thenReturn(pharmacyResponse);

        RegisterPharmacistResponseDto actual = onboardingService.registerPharmacist(request);

        ArgumentCaptor<RegisterRequestDto> pharmacistCaptor = ArgumentCaptor.forClass(RegisterRequestDto.class);
        verify(identityService).register(pharmacistCaptor.capture());
        assertEquals("pharmacist@tenamed.com", pharmacistCaptor.getValue().getEmail());
        assertEquals(Set.of("PHARMACIST"), pharmacistCaptor.getValue().getRoleNames());

        ArgumentCaptor<CreatePharmacyRequest> pharmacyCaptor = ArgumentCaptor.forClass(CreatePharmacyRequest.class);
        verify(pharmacyService).createPharmacy(pharmacyCaptor.capture());
        assertEquals("https://supabase.example/license.png", pharmacyCaptor.getValue().getLicenseImageUrl());
        assertEquals(pharmacistUserId, pharmacyCaptor.getValue().getOwnerId());
        assertEquals("City Pharmacy", actual.getPharmacy().getName());
        assertEquals(pharmacistUserId, actual.getPharmacist().getUserId());
    }

    @Test
    void shouldRejectWhenLicenseImageMissing() {
        RegisterPharmacistRequestDto request = new RegisterPharmacistRequestDto();
        request.setPharmacist(new RegisterPharmacistRequestDto.OwnerDto());
        request.setPharmacy(new RegisterPharmacistRequestDto.PharmacyDto());

        assertThrows(BadRequestException.class, () -> onboardingService.registerPharmacist(request));
    }
}
