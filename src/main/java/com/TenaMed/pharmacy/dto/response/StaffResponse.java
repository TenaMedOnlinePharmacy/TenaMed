package com.TenaMed.pharmacy.dto.response;

import com.TenaMed.pharmacy.enums.EmploymentStatus;
import com.TenaMed.pharmacy.enums.StaffRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class StaffResponse {

    private UUID id;
    private UUID userId;
    private UUID pharmacyId;
    private StaffRole staffRole;
    private EmploymentStatus employmentStatus;
    private String licenseNumber;
    private LocalDate licenseExpiry;
    private Boolean canVerifyPrescriptions;
    private Boolean canManageInventory;
    private UUID verifiedBy;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isVerified;
}