package com.TenaMed.pharmacy.mapper;

import com.TenaMed.pharmacy.dto.request.AddStaffRequest;
import com.TenaMed.pharmacy.dto.response.StaffResponse;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.entity.UserPharmacy;
import org.springframework.stereotype.Component;

@Component
public class UserPharmacyMapper {

    public UserPharmacy toEntity(AddStaffRequest request, Pharmacy pharmacy) {
        UserPharmacy userPharmacy = new UserPharmacy();
        userPharmacy.setUserId(request.getUserId());
        userPharmacy.setPharmacy(pharmacy);
        userPharmacy.setStaffRole(request.getStaffRole());
        userPharmacy.setEmploymentStatus(request.getEmploymentStatus());
        userPharmacy.setLicenseNumber(request.getLicenseNumber());
        userPharmacy.setLicenseExpiry(request.getLicenseExpiry());
        userPharmacy.setCanVerifyPrescriptions(request.getCanVerifyPrescriptions());
        userPharmacy.setCanManageInventory(request.getCanManageInventory());
        return userPharmacy;
    }

    public StaffResponse toResponse(UserPharmacy userPharmacy, String firstName, String lastName) {
        return StaffResponse.builder()
            .id(userPharmacy.getId())
            .userId(userPharmacy.getUserId())
            .pharmacyId(userPharmacy.getPharmacy().getId())
            .staffRole(userPharmacy.getStaffRole())
            .employmentStatus(userPharmacy.getEmploymentStatus())
            .licenseNumber(userPharmacy.getLicenseNumber())
            .licenseExpiry(userPharmacy.getLicenseExpiry())
            .canVerifyPrescriptions(userPharmacy.getCanVerifyPrescriptions())
            .canManageInventory(userPharmacy.getCanManageInventory())
            .verifiedBy(userPharmacy.getVerifiedBy())
            .verifiedAt(userPharmacy.getVerifiedAt())
            .createdAt(userPharmacy.getCreatedAt())
            .updatedAt(userPharmacy.getUpdatedAt())
            .isVerified(userPharmacy.getIsVerified())
            .firstName(firstName)
            .lastName(lastName)
            .build();
    }
}