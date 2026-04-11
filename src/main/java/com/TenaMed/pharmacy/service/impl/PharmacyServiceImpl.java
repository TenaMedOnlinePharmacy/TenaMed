package com.TenaMed.pharmacy.service.impl;

import com.TenaMed.pharmacy.dto.request.CreatePharmacyRequest;
import com.TenaMed.pharmacy.dto.response.PharmacyResponse;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.enums.PharmacyStatus;
import com.TenaMed.pharmacy.exception.PharmacyNotFoundException;
import com.TenaMed.pharmacy.exception.PharmacyValidationException;
import com.TenaMed.pharmacy.mapper.PharmacyMapper;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
import com.TenaMed.pharmacy.service.PharmacyService;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class PharmacyServiceImpl implements PharmacyService {

    private final PharmacyRepository pharmacyRepository;
    private final PharmacyMapper pharmacyMapper;

    public PharmacyServiceImpl(PharmacyRepository pharmacyRepository,
                               PharmacyMapper pharmacyMapper) {
        this.pharmacyRepository = pharmacyRepository;
        this.pharmacyMapper = pharmacyMapper;
    }

    @Override
    public PharmacyResponse createPharmacy(CreatePharmacyRequest request) {
        validateCreateRequest(request);

        String normalizedName = normalizeRequired(request.getName(), "name");
        String normalizedLegalName = normalizeRequired(request.getLegalName(), "legalName");
        String normalizedLicense = normalizeRequired(request.getLicenseNumber(), "licenseNumber");
        String normalizedEmail = normalizeRequired(request.getEmail(), "email").toLowerCase(Locale.ROOT);
        String normalizedPhone = normalizeRequired(request.getPhone(), "phone");

        if (pharmacyRepository.existsByLicenseNumberIgnoreCase(normalizedLicense)) {
            throw new PharmacyValidationException("licenseNumber already exists");
        }
        if (pharmacyRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new PharmacyValidationException("email already exists");
        }
        if (pharmacyRepository.existsByPhone(normalizedPhone)) {
            throw new PharmacyValidationException("phone already exists");
        }

        request.setName(normalizedName);
        request.setLegalName(normalizedLegalName);
        request.setLicenseNumber(normalizedLicense);
        request.setEmail(normalizedEmail);
        request.setPhone(normalizedPhone);

        Pharmacy pharmacy = pharmacyMapper.toEntity(request);
        Pharmacy saved = pharmacyRepository.save(pharmacy);
        return pharmacyMapper.toResponse(saved);
    }

    @Override
    public PharmacyResponse verifyPharmacy(UUID pharmacyId) {
        AuthenticatedUserPrincipal principal = authenticatedPrincipal();
        if (principal.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            throw new PharmacyValidationException("Only admin users can verify pharmacies");
        }

        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
            .orElseThrow(() -> new PharmacyNotFoundException(pharmacyId));
        pharmacy.setStatus(PharmacyStatus.VERIFIED);
        pharmacy.setVerifiedBy(principal.getUserId());
        pharmacy.setVerifiedAt(LocalDateTime.now());
        return pharmacyMapper.toResponse(pharmacyRepository.save(pharmacy));
    }

    @Override
    @Transactional(readOnly = true)
    public PharmacyResponse getPharmacy(UUID pharmacyId) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
            .orElseThrow(() -> new PharmacyNotFoundException(pharmacyId));
        return pharmacyMapper.toResponse(pharmacy);
    }

    private void validateCreateRequest(CreatePharmacyRequest request) {
        if (request == null) {
            throw new PharmacyValidationException("request is required");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new PharmacyValidationException(fieldName + " is required");
        }
        return value.trim();
    }

    private AuthenticatedUserPrincipal authenticatedPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new PharmacyValidationException("Authenticated user is required");
        }
        return principal;
    }
}