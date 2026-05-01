package com.TenaMed.pharmacy.service.impl;

import com.TenaMed.pharmacy.dto.request.CreatePharmacyRequest;
import com.TenaMed.pharmacy.dto.response.PharmacyResponse;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.enums.PharmacyStatus;
import com.TenaMed.pharmacy.exception.PharmacyNotFoundException;
import com.TenaMed.pharmacy.exception.PharmacyValidationException;
import com.TenaMed.invitation.dto.InvitationResponseDto;
import com.TenaMed.invitation.service.InvitationService;
import com.TenaMed.events.DomainEventService;
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
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class PharmacyServiceImpl implements PharmacyService {

    private final PharmacyRepository pharmacyRepository;
    private final PharmacyMapper pharmacyMapper;
    private final InvitationService invitationService;
    private final DomainEventService domainEventService;

    public PharmacyServiceImpl(PharmacyRepository pharmacyRepository,
                               PharmacyMapper pharmacyMapper,
                               InvitationService invitationService,
                               DomainEventService domainEventService) {
        this.pharmacyRepository = pharmacyRepository;
        this.pharmacyMapper = pharmacyMapper;
        this.invitationService = invitationService;
        this.domainEventService = domainEventService;
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
        domainEventService.publish(
            "PHARMACY_CREATED",
            "PHARMACY",
            saved.getId(),
            "PHARMACY_OWNER",
            saved.getOwnerId(),
            "PHARMACY",
            saved.getId(),
            Map.of("status", String.valueOf(saved.getStatus()))
        );
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
            
        if (pharmacy.getStatus() == PharmacyStatus.VERIFIED) {
            throw new PharmacyValidationException("Pharmacy is already VERIFIED");
        }
        
        pharmacy.setStatus(PharmacyStatus.VERIFIED);
        pharmacy.setVerifiedBy(principal.getUserId());
        pharmacy.setVerifiedAt(LocalDateTime.now());
        Pharmacy saved = pharmacyRepository.save(pharmacy);
        domainEventService.publish(
            "PHARMACY_VERIFIED",
            "PHARMACY",
            saved.getId(),
            "ADMIN",
            principal.getUserId(),
            "PHARMACY",
            saved.getId(),
            Map.of("status", saved.getStatus().name())
        );
        return pharmacyMapper.toResponse(saved);
    }

    @Override
    public PharmacyResponse rejectPharmacy(UUID pharmacyId) {
        AuthenticatedUserPrincipal principal = authenticatedPrincipal();
        if (principal.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            throw new PharmacyValidationException("Only admin users can reject pharmacies");
        }

        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
            .orElseThrow(() -> new PharmacyNotFoundException(pharmacyId));
            
        if (pharmacy.getStatus() == PharmacyStatus.REJECTED) {
            throw new PharmacyValidationException("Pharmacy is already REJECTED");
        }
        
        pharmacy.setStatus(PharmacyStatus.REJECTED);
        Pharmacy saved = pharmacyRepository.save(pharmacy);
        domainEventService.publish(
            "PHARMACY_REJECTED",
            "PHARMACY",
            saved.getId(),
            "ADMIN",
            principal.getUserId(),
            "PHARMACY",
            saved.getId(),
            Map.of("status", saved.getStatus().name())
        );
        return pharmacyMapper.toResponse(saved);
    }

    @Override
    public PharmacyResponse suspendPharmacy(UUID pharmacyId) {
        AuthenticatedUserPrincipal principal = authenticatedPrincipal();
        if (principal.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            throw new PharmacyValidationException("Only admin users can suspend pharmacies");
        }

        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
            .orElseThrow(() -> new PharmacyNotFoundException(pharmacyId));
            
        if (pharmacy.getStatus() == PharmacyStatus.SUSPENDED) {
            throw new PharmacyValidationException("Pharmacy is already SUSPENDED");
        }
        
        pharmacy.setStatus(PharmacyStatus.SUSPENDED);
        Pharmacy saved = pharmacyRepository.save(pharmacy);
        domainEventService.publish(
            "PHARMACY_SUSPENDED",
            "PHARMACY",
            saved.getId(),
            "ADMIN",
            principal.getUserId(),
            "PHARMACY",
            saved.getId(),
            Map.of("status", saved.getStatus().name())
        );
        return pharmacyMapper.toResponse(saved);
    }

    @Override
    public PharmacyResponse unsuspendPharmacy(UUID pharmacyId) {
        AuthenticatedUserPrincipal principal = authenticatedPrincipal();
        if (principal.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            throw new PharmacyValidationException("Only admin users can unsuspend pharmacies");
        }

        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
            .orElseThrow(() -> new PharmacyNotFoundException(pharmacyId));
            
        if (pharmacy.getStatus() != PharmacyStatus.SUSPENDED) {
            throw new PharmacyValidationException("Pharmacy is not SUSPENDED");
        }
        
        pharmacy.setStatus(PharmacyStatus.VERIFIED);
        Pharmacy saved = pharmacyRepository.save(pharmacy);
        domainEventService.publish(
            "PHARMACY_UNSUSPENDED",
            "PHARMACY",
            saved.getId(),
            "ADMIN",
            principal.getUserId(),
            "PHARMACY",
            saved.getId(),
            Map.of("status", saved.getStatus().name())
        );
        return pharmacyMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PharmacyResponse getPharmacy(UUID pharmacyId) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
            .orElseThrow(() -> new PharmacyNotFoundException(pharmacyId));
        return pharmacyMapper.toResponse(pharmacy);
    }

    @Override
    public InvitationResponseDto invitePharmacist(UUID pharmacyId, String email) {
        if (pharmacyId == null) {
            throw new PharmacyValidationException("pharmacyId is required");
        }
        if (email == null || email.isBlank()) {
            throw new PharmacyValidationException("email is required");
        }

        AuthenticatedUserPrincipal principal = authenticatedPrincipal();
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new PharmacyNotFoundException(pharmacyId));

        boolean isOwner = principal.getUserId().equals(pharmacy.getOwnerId());
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isOwner && !isAdmin) {
            throw new PharmacyValidationException("Only pharmacy owner or admin can invite pharmacists");
        }
        if (pharmacy.getStatus() != PharmacyStatus.VERIFIED) {
            throw new PharmacyValidationException("Pharmacy must be VERIFIED before inviting pharmacists");
        }

        InvitationResponseDto invitation = invitationService.createPharmacistInvitation(pharmacyId, email.trim());
        domainEventService.publish(
            "PHARMACY_PHARMACIST_INVITATION_REQUESTED",
            "INVITATION",
            invitation.getId(),
            "PHARMACY",
            pharmacyId,
            Map.of("email", email.trim())
        );
        return invitation;
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