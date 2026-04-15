package com.TenaMed.pharmacy.dto.response;

import com.TenaMed.pharmacy.enums.PharmacyStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PharmacyResponse {

    private UUID id;
    private String name;
    private String legalName;
    private String licenseNumber;
    private String licenseImageUrl;
    private String email;
    private String phone;
    private String website;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String pharmacyType;
    private String operatingHours;
    private Boolean is24Hours;
    private Boolean hasDelivery;
    private PharmacyStatus status;
    private UUID ownerId;
    private UUID verifiedBy;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}