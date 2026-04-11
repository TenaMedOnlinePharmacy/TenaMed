package com.TenaMed.pharmacy.service;

import com.TenaMed.pharmacy.dto.request.CreatePharmacyRequest;
import com.TenaMed.pharmacy.dto.response.PharmacyResponse;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.enums.PharmacyStatus;
import com.TenaMed.pharmacy.exception.PharmacyNotFoundException;
import com.TenaMed.pharmacy.mapper.PharmacyMapper;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
import com.TenaMed.pharmacy.service.impl.PharmacyServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PharmacyServiceImplTests {

    @Mock
    private PharmacyRepository pharmacyRepository;

    @Mock
    private PharmacyMapper pharmacyMapper;

    @InjectMocks
    private PharmacyServiceImpl pharmacyService;

    @Test
    void shouldCreatePharmacy() {
        CreatePharmacyRequest request = new CreatePharmacyRequest();
        Pharmacy entity = new Pharmacy();
        Pharmacy saved = new Pharmacy();
        saved.setId(UUID.randomUUID());
        PharmacyResponse response = PharmacyResponse.builder().id(saved.getId()).build();

        when(pharmacyMapper.toEntity(request)).thenReturn(entity);
        when(pharmacyRepository.save(entity)).thenReturn(saved);
        when(pharmacyMapper.toResponse(saved)).thenReturn(response);

        PharmacyResponse actual = pharmacyService.createPharmacy(request);

        assertEquals(saved.getId(), actual.getId());
    }

    @Test
    void shouldVerifyPharmacy() {
        UUID pharmacyId = UUID.randomUUID();
        UUID verifier = UUID.randomUUID();
        Pharmacy pharmacy = new Pharmacy();
        PharmacyResponse response = PharmacyResponse.builder().status(PharmacyStatus.VERIFIED).build();

        when(pharmacyRepository.findById(pharmacyId)).thenReturn(Optional.of(pharmacy));
        when(pharmacyRepository.save(any(Pharmacy.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pharmacyMapper.toResponse(any(Pharmacy.class))).thenReturn(response);

        PharmacyResponse actual = pharmacyService.verifyPharmacy(pharmacyId, verifier);

        assertEquals(PharmacyStatus.VERIFIED, actual.getStatus());
    }

    @Test
    void shouldThrowWhenPharmacyNotFound() {
        UUID pharmacyId = UUID.randomUUID();
        when(pharmacyRepository.findById(pharmacyId)).thenReturn(Optional.empty());

        assertThrows(PharmacyNotFoundException.class, () -> pharmacyService.getPharmacy(pharmacyId));
    }
}