package com.TenaMed.pharmacy.service;

import com.TenaMed.pharmacy.dto.request.CreatePharmacyRequest;
import com.TenaMed.pharmacy.dto.response.PharmacyResponse;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.enums.PharmacyStatus;
import com.TenaMed.pharmacy.exception.PharmacyNotFoundException;
import com.TenaMed.pharmacy.exception.PharmacyValidationException;
import com.TenaMed.pharmacy.mapper.PharmacyMapper;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
import com.TenaMed.pharmacy.service.impl.PharmacyServiceImpl;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

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
        request.setName("Pharmacy 1");
        request.setLegalName("Pharmacy 1 LLC");
        request.setLicenseNumber("LIC-10");
        request.setEmail("test@pharmacy.com");
        request.setPhone("+251900111111");
        Pharmacy entity = new Pharmacy();
        Pharmacy saved = new Pharmacy();
        saved.setId(UUID.randomUUID());
        PharmacyResponse response = PharmacyResponse.builder().id(saved.getId()).build();

        when(pharmacyRepository.existsByLicenseNumberIgnoreCase("LIC-10")).thenReturn(false);
        when(pharmacyRepository.existsByEmailIgnoreCase("test@pharmacy.com")).thenReturn(false);
        when(pharmacyRepository.existsByPhone("+251900111111")).thenReturn(false);

        when(pharmacyMapper.toEntity(request)).thenReturn(entity);
        when(pharmacyRepository.save(entity)).thenReturn(saved);
        when(pharmacyMapper.toResponse(saved)).thenReturn(response);

        PharmacyResponse actual = pharmacyService.createPharmacy(request);

        assertEquals(saved.getId(), actual.getId());
    }

    @Test
    void shouldRejectCreateWhenEmailExists() {
        CreatePharmacyRequest request = new CreatePharmacyRequest();
        request.setName("Pharmacy 1");
        request.setLegalName("Pharmacy 1 LLC");
        request.setLicenseNumber("LIC-10");
        request.setEmail("test@pharmacy.com");
        request.setPhone("+251900111111");

        when(pharmacyRepository.existsByLicenseNumberIgnoreCase("LIC-10")).thenReturn(false);
        when(pharmacyRepository.existsByEmailIgnoreCase("test@pharmacy.com")).thenReturn(true);

        assertThrows(PharmacyValidationException.class, () -> pharmacyService.createPharmacy(request));
    }

    @Test
    void shouldVerifyPharmacy() {
        UUID pharmacyId = UUID.randomUUID();
        UUID verifier = UUID.randomUUID();
        Pharmacy pharmacy = new Pharmacy();
        PharmacyResponse response = PharmacyResponse.builder().status(PharmacyStatus.VERIFIED).build();

        setPrincipal(verifier, "ROLE_ADMIN");

        when(pharmacyRepository.findById(pharmacyId)).thenReturn(Optional.of(pharmacy));
        when(pharmacyRepository.save(any(Pharmacy.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pharmacyMapper.toResponse(any(Pharmacy.class))).thenReturn(response);

        PharmacyResponse actual = pharmacyService.verifyPharmacy(pharmacyId);

        assertEquals(PharmacyStatus.VERIFIED, actual.getStatus());
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectVerifyWhenUserIsNotAdmin() {
        UUID pharmacyId = UUID.randomUUID();
        Pharmacy pharmacy = new Pharmacy();
        setPrincipal(UUID.randomUUID(), "ROLE_PHARMACIST");

        when(pharmacyRepository.findById(pharmacyId)).thenReturn(Optional.of(pharmacy));

        assertThrows(PharmacyValidationException.class, () -> pharmacyService.verifyPharmacy(pharmacyId));
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldThrowWhenPharmacyNotFound() {
        UUID pharmacyId = UUID.randomUUID();
        when(pharmacyRepository.findById(pharmacyId)).thenReturn(Optional.empty());

        assertThrows(PharmacyNotFoundException.class, () -> pharmacyService.getPharmacy(pharmacyId));
    }

    private void setPrincipal(UUID userId, String authority) {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
            userId,
            UUID.randomUUID(),
            "admin@tenamed.com",
            "pwd",
            java.util.List.of(new SimpleGrantedAuthority(authority)),
            true
        );
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}