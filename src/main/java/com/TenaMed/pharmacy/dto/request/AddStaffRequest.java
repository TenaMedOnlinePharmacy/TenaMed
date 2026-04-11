package com.TenaMed.pharmacy.dto.request;

import com.TenaMed.pharmacy.enums.EmploymentStatus;
import com.TenaMed.pharmacy.enums.StaffRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class AddStaffRequest {

    @NotNull
    private UUID userId;

    private UUID pharmacyId;

    @NotNull
    private StaffRole staffRole;

    @NotNull
    private EmploymentStatus employmentStatus;

    private String licenseNumber;

    private LocalDate licenseExpiry;

    private Boolean canVerifyPrescriptions;

    private Boolean canManageInventory;
}