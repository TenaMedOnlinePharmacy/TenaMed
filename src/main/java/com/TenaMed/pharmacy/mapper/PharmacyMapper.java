package com.TenaMed.pharmacy.mapper;

import com.TenaMed.pharmacy.dto.request.CreatePharmacyRequest;
import com.TenaMed.pharmacy.dto.response.PharmacyResponse;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.enums.PharmacyStatus;
import org.springframework.stereotype.Component;

@Component
public class PharmacyMapper {

    public Pharmacy toEntity(CreatePharmacyRequest request) {
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setName(request.getName());
        pharmacy.setLegalName(request.getLegalName());
        pharmacy.setLicenseNumber(request.getLicenseNumber());
        pharmacy.setEmail(request.getEmail());
        pharmacy.setPhone(request.getPhone());
        pharmacy.setWebsite(request.getWebsite());
        pharmacy.setAddressLine1(request.getAddressLine1());
        pharmacy.setAddressLine2(request.getAddressLine2());
        pharmacy.setCity(request.getCity());
        pharmacy.setPharmacyType(request.getPharmacyType());
        pharmacy.setOperatingHours(request.getOperatingHours());
        pharmacy.setIs24Hours(request.getIs24Hours());
        pharmacy.setHasDelivery(request.getHasDelivery());
        pharmacy.setOwnerId(request.getOwnerId());
        pharmacy.setStatus(PharmacyStatus.PENDING);
        return pharmacy;
    }

    public PharmacyResponse toResponse(Pharmacy pharmacy) {
        return PharmacyResponse.builder()
            .id(pharmacy.getId())
            .name(pharmacy.getName())
            .legalName(pharmacy.getLegalName())
            .licenseNumber(pharmacy.getLicenseNumber())
            .email(pharmacy.getEmail())
            .phone(pharmacy.getPhone())
            .website(pharmacy.getWebsite())
            .addressLine1(pharmacy.getAddressLine1())
            .addressLine2(pharmacy.getAddressLine2())
            .city(pharmacy.getCity())
            .pharmacyType(pharmacy.getPharmacyType())
            .operatingHours(pharmacy.getOperatingHours())
            .is24Hours(pharmacy.getIs24Hours())
            .hasDelivery(pharmacy.getHasDelivery())
            .status(pharmacy.getStatus())
            .ownerId(pharmacy.getOwnerId())
            .verifiedBy(pharmacy.getVerifiedBy())
            .verifiedAt(pharmacy.getVerifiedAt())
            .createdAt(pharmacy.getCreatedAt())
            .updatedAt(pharmacy.getUpdatedAt())
            .build();
    }
}