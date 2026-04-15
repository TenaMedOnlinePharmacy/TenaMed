package com.TenaMed.user.service.impl;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.hospital.dto.HospitalRequestDto;
import com.TenaMed.hospital.dto.HospitalResponseDto;
import com.TenaMed.hospital.service.HospitalService;
import com.TenaMed.ocr.service.SupabaseStorageService;
import com.TenaMed.user.dto.RegisterHospitalOwnerRequestDto;
import com.TenaMed.user.dto.RegisterHospitalOwnerResponseDto;
import com.TenaMed.user.dto.RegisterRequestDto;
import com.TenaMed.user.dto.RegisterResponseDto;
import com.TenaMed.user.service.HospitalOwnerOnboardingService;
import com.TenaMed.user.service.IdentityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class HospitalOwnerOnboardingServiceImpl implements HospitalOwnerOnboardingService {

    private static final String HOSPITAL_OWNER_ROLE = "HOSPITAL_OWNER";

    private final IdentityService identityService;
    private final HospitalService hospitalService;
    private final SupabaseStorageService supabaseStorageService;

    public HospitalOwnerOnboardingServiceImpl(IdentityService identityService,
                                              HospitalService hospitalService,
                                              SupabaseStorageService supabaseStorageService) {
        this.identityService = identityService;
        this.hospitalService = hospitalService;
        this.supabaseStorageService = supabaseStorageService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterHospitalOwnerResponseDto registerHospitalOwner(RegisterHospitalOwnerRequestDto requestDto) {
        validateRequest(requestDto);

        identityService.populateRoles();

        RegisterRequestDto ownerRequest = new RegisterRequestDto();
        ownerRequest.setEmail(requestDto.getOwner().getEmail());
        ownerRequest.setPassword(requestDto.getOwner().getPassword());
        ownerRequest.setFirstName(requestDto.getOwner().getFirstName());
        ownerRequest.setLastName(requestDto.getOwner().getLastName());
        ownerRequest.setPhone(requestDto.getOwner().getPhone());
        ownerRequest.setRoleNames(Set.of(HOSPITAL_OWNER_ROLE));

        RegisterResponseDto ownerResponse = identityService.register(ownerRequest);

        String licenseImageUrl = supabaseStorageService.uploadAndGetSignedUrl(requestDto.getHospital().getLicenseImage());

        HospitalRequestDto hospitalRequest = new HospitalRequestDto();
        hospitalRequest.setName(requestDto.getHospital().getName());
        hospitalRequest.setLicenseNumber(requestDto.getHospital().getLicenseNumber());
        hospitalRequest.setLicenseImageUrl(licenseImageUrl);

        HospitalResponseDto hospitalResponse = hospitalService.createHospitalForOwner(hospitalRequest, ownerResponse.getUserId());

        return new RegisterHospitalOwnerResponseDto(ownerResponse, hospitalResponse);
    }

    private void validateRequest(RegisterHospitalOwnerRequestDto requestDto) {
        if (requestDto == null || requestDto.getOwner() == null || requestDto.getHospital() == null) {
            throw new BadRequestException("Owner and hospital payload are required");
        }
        if (requestDto.getHospital().getLicenseImage() == null || requestDto.getHospital().getLicenseImage().isEmpty()) {
            throw new BadRequestException("Hospital license image is required");
        }
    }
}
