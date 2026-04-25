package com.TenaMed.pharmacy.mapper;

import com.TenaMed.pharmacy.dto.request.AddStaffRequest;
import com.TenaMed.pharmacy.dto.response.StaffResponse;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.entity.UserPharmacy;
import com.TenaMed.pharmacy.enums.EmploymentStatus;
import com.TenaMed.pharmacy.enums.StaffRole;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserPharmacyMapperTests {

    private final UserPharmacyMapper userPharmacyMapper = new UserPharmacyMapper();

    @Test
    void shouldMapAddStaffRequestToEntity() {
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setId(UUID.randomUUID());

        AddStaffRequest request = new AddStaffRequest();
        request.setUserId(UUID.randomUUID());
        request.setPharmacyId(pharmacy.getId());
        request.setStaffRole(StaffRole.PHARMACIST);
        request.setEmploymentStatus(EmploymentStatus.ACTIVE);

        UserPharmacy userPharmacy = userPharmacyMapper.toEntity(request, pharmacy);

        assertEquals(request.getUserId(), userPharmacy.getUserId());
        assertEquals(StaffRole.PHARMACIST, userPharmacy.getStaffRole());
        assertEquals(EmploymentStatus.ACTIVE, userPharmacy.getEmploymentStatus());
    }

    @Test
    void shouldMapEntityToStaffResponse() {
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setId(UUID.randomUUID());

        UserPharmacy userPharmacy = new UserPharmacy();
        userPharmacy.setId(UUID.randomUUID());
        userPharmacy.setUserId(UUID.randomUUID());
        userPharmacy.setPharmacy(pharmacy);
        userPharmacy.setStaffRole(StaffRole.OWNER);
        userPharmacy.setEmploymentStatus(EmploymentStatus.ACTIVE);

        String firstName = "John";
        String lastName = "Doe";
        StaffResponse response = userPharmacyMapper.toResponse(userPharmacy, firstName, lastName);

        assertEquals(userPharmacy.getId(), response.getId());
        assertEquals(pharmacy.getId(), response.getPharmacyId());
        assertEquals(StaffRole.OWNER, response.getStaffRole());
        assertEquals(firstName, response.getFirstName());
        assertEquals(lastName, response.getLastName());
    }
}