package com.TenaMed.pharmacy.mapper;

import com.TenaMed.pharmacy.dto.request.CreatePharmacyRequest;
import com.TenaMed.pharmacy.dto.response.PharmacyResponse;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.enums.PharmacyStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PharmacyMapperTests {

    private final PharmacyMapper pharmacyMapper = new PharmacyMapper();

    @Test
    void shouldMapCreateRequestToEntity() {
        CreatePharmacyRequest request = new CreatePharmacyRequest();
        request.setName("Test Pharmacy");
        request.setLicenseNumber("LIC-123");
        request.setEmail("test@pharmacy.com");
        request.setPhone("+251900000001");
        request.setOwnerId(UUID.randomUUID());

        Pharmacy pharmacy = pharmacyMapper.toEntity(request);

        assertEquals("Test Pharmacy", pharmacy.getName());
        assertEquals("LIC-123", pharmacy.getLicenseNumber());
        assertEquals(PharmacyStatus.PENDING, pharmacy.getStatus());
        assertEquals(request.getOwnerId(), pharmacy.getOwnerId());
    }

    @Test
    void shouldMapEntityToResponse() {
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setId(UUID.randomUUID());
        pharmacy.setName("Response Pharmacy");
        pharmacy.setStatus(PharmacyStatus.VERIFIED);
        pharmacy.setOwnerId(UUID.randomUUID());

        PharmacyResponse response = pharmacyMapper.toResponse(pharmacy);

        assertEquals(pharmacy.getId(), response.getId());
        assertEquals("Response Pharmacy", response.getName());
        assertEquals(PharmacyStatus.VERIFIED, response.getStatus());
    }
}