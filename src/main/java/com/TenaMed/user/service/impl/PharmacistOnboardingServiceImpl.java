package com.TenaMed.user.service.impl;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.ocr.service.SupabaseStorageService;
import com.TenaMed.pharmacy.dto.request.CreatePharmacyRequest;
import com.TenaMed.pharmacy.dto.response.PharmacyResponse;
import com.TenaMed.pharmacy.service.PharmacyService;
import com.TenaMed.user.dto.RegisterPharmacistRequestDto;
import com.TenaMed.user.dto.RegisterPharmacistResponseDto;
import com.TenaMed.user.dto.RegisterRequestDto;
import com.TenaMed.user.dto.RegisterResponseDto;
import com.TenaMed.user.service.IdentityService;
import com.TenaMed.user.service.PharmacistOnboardingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class PharmacistOnboardingServiceImpl implements PharmacistOnboardingService {

    private static final String PHARMACIST_ROLE = "PHARMACIST";

    private final IdentityService identityService;
    private final PharmacyService pharmacyService;
    private final SupabaseStorageService supabaseStorageService;

    public PharmacistOnboardingServiceImpl(IdentityService identityService,
                                           PharmacyService pharmacyService,
                                           SupabaseStorageService supabaseStorageService) {
        this.identityService = identityService;
        this.pharmacyService = pharmacyService;
        this.supabaseStorageService = supabaseStorageService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterPharmacistResponseDto registerPharmacist(RegisterPharmacistRequestDto requestDto) {
        validateRequest(requestDto);

        identityService.populateRoles();

        RegisterRequestDto pharmacistRequest = new RegisterRequestDto();
        pharmacistRequest.setEmail(requestDto.getPharmacist().getEmail());
        pharmacistRequest.setPassword(requestDto.getPharmacist().getPassword());
        pharmacistRequest.setFirstName(requestDto.getPharmacist().getFirstName());
        pharmacistRequest.setLastName(requestDto.getPharmacist().getLastName());
        pharmacistRequest.setPhone(requestDto.getPharmacist().getPhone());
        pharmacistRequest.setRoleNames(Set.of(PHARMACIST_ROLE));

        RegisterResponseDto pharmacistResponse = identityService.register(pharmacistRequest);

        String licenseImageUrl = supabaseStorageService.uploadAndGetSignedUrl(requestDto.getPharmacy().getLicenseImage());

        CreatePharmacyRequest pharmacyRequest = new CreatePharmacyRequest();
        pharmacyRequest.setName(requestDto.getPharmacy().getName());
        pharmacyRequest.setLegalName(requestDto.getPharmacy().getLegalName());
        pharmacyRequest.setLicenseNumber(requestDto.getPharmacy().getLicenseNumber());
        pharmacyRequest.setEmail(requestDto.getPharmacy().getEmail());
        pharmacyRequest.setPhone(requestDto.getPharmacy().getPhone());
        pharmacyRequest.setWebsite(requestDto.getPharmacy().getWebsite());
        pharmacyRequest.setAddressLine1(requestDto.getPharmacy().getAddressLine1());
        pharmacyRequest.setAddressLine2(requestDto.getPharmacy().getAddressLine2());
        pharmacyRequest.setCity(requestDto.getPharmacy().getCity());
        pharmacyRequest.setPharmacyType(requestDto.getPharmacy().getPharmacyType());
        pharmacyRequest.setOperatingHours(requestDto.getPharmacy().getOperatingHours());
        pharmacyRequest.setIs24Hours(requestDto.getPharmacy().getIs24Hours());
        pharmacyRequest.setHasDelivery(requestDto.getPharmacy().getHasDelivery());
        pharmacyRequest.setLicenseImageUrl(licenseImageUrl);
        pharmacyRequest.setOwnerId(pharmacistResponse.getUserId());

        PharmacyResponse pharmacyResponse = pharmacyService.createPharmacy(pharmacyRequest);

        return new RegisterPharmacistResponseDto(pharmacistResponse, pharmacyResponse);
    }

    private void validateRequest(RegisterPharmacistRequestDto requestDto) {
        if (requestDto == null || requestDto.getPharmacist() == null || requestDto.getPharmacy() == null) {
            throw new BadRequestException("Pharmacist and pharmacy payload are required");
        }
        if (requestDto.getPharmacy().getLicenseImage() == null || requestDto.getPharmacy().getLicenseImage().isEmpty()) {
            throw new BadRequestException("Pharmacy license image is required");
        }
    }
}