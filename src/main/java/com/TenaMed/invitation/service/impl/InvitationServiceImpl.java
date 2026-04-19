package com.TenaMed.invitation.service.impl;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.common.exception.ResourceNotFoundException;
import com.TenaMed.email.dto.EmailRequest;
import com.TenaMed.email.service.EmailService;
import com.TenaMed.email.service.EmailTemplateBuilder;
import com.TenaMed.hospital.entity.Hospital;
import com.TenaMed.hospital.repository.HospitalRepository;
import com.TenaMed.invitation.dto.InvitationResponseDto;
import com.TenaMed.invitation.entity.Invitation;
import com.TenaMed.invitation.entity.InvitationInstituteType;
import com.TenaMed.invitation.entity.InvitationRole;
import com.TenaMed.invitation.entity.InvitationStatus;
import com.TenaMed.invitation.mapper.InvitationMapper;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
import com.TenaMed.invitation.repository.InvitationRepository;
import com.TenaMed.invitation.service.InvitationService;
import com.TenaMed.events.DomainEventService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class InvitationServiceImpl implements InvitationService {

    private static final long INVITATION_TTL_HOURS = 24L;
    private static final String DOCTOR_INVITE_SUBJECT = "You're invited to join a hospital";
    private static final String PHARMACIST_INVITE_SUBJECT = "You're invited to join a pharmacy";

    private final InvitationRepository invitationRepository;
    private final InvitationMapper invitationMapper;
    private final HospitalRepository hospitalRepository;
    private final PharmacyRepository pharmacyRepository;
    private final EmailService emailService;
    private final EmailTemplateBuilder emailTemplateBuilder;
    private final DomainEventService domainEventService;

    @Value("${app.invitation.base-url:http://localhost:8080/api/invitations}")
    private String invitationBaseUrl;

    public InvitationServiceImpl(InvitationRepository invitationRepository,
                                 InvitationMapper invitationMapper,
                                 HospitalRepository hospitalRepository,
                                 PharmacyRepository pharmacyRepository,
                                 EmailService emailService,
                                 EmailTemplateBuilder emailTemplateBuilder,
                                 DomainEventService domainEventService) {
        this.invitationRepository = invitationRepository;
        this.invitationMapper = invitationMapper;
        this.hospitalRepository = hospitalRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.emailService = emailService;
        this.emailTemplateBuilder = emailTemplateBuilder;
        this.domainEventService = domainEventService;
    }

    @Override
    @Transactional
    public InvitationResponseDto createDoctorInvitation(UUID hospitalId, String email) {
        if (hospitalId == null) {
            throw new BadRequestException("hospitalId is required");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new BadRequestException("email is required");
        }

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found: " + hospitalId));

        Invitation invitation = new Invitation();
        invitation.setEmail(email.trim().toLowerCase());
        invitation.setRole(InvitationRole.DOCTOR);
        invitation.setInstituteId(hospitalId);
        invitation.setInstituteType(InvitationInstituteType.HOSPITAL);
        invitation.setHospitalId(hospitalId);
        invitation.setPharmacyId(null);
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(INVITATION_TTL_HOURS));

        Invitation saved = invitationRepository.saveAndFlush(invitation);
        InvitationResponseDto response = invitationMapper.toResponse(saved);

        String invitationLink = buildInvitationLink(saved.getToken());
        String body = emailTemplateBuilder.buildDoctorInvitationEmail(hospital.getName(), invitationLink);
        emailService.sendEmail(new EmailRequest(saved.getEmail(), DOCTOR_INVITE_SUBJECT, body, true));

        domainEventService.publish(
            "INVITATION_CREATED",
            "INVITATION",
            saved.getId(),
            "HOSPITAL",
            hospitalId,
            Map.of("role", saved.getRole().name(), "email", saved.getEmail())
        );

        return response;
    }

    @Override
    @Transactional
    public InvitationResponseDto createPharmacistInvitation(UUID pharmacyId, String email) {
        if (pharmacyId == null) {
            throw new BadRequestException("pharmacyId is required");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new BadRequestException("email is required");
        }

        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy not found: " + pharmacyId));

        Invitation invitation = new Invitation();
        invitation.setEmail(email.trim().toLowerCase());
        invitation.setRole(InvitationRole.PHARMACIST);
        invitation.setInstituteId(pharmacyId);
        invitation.setInstituteType(InvitationInstituteType.PHARMACY);
        invitation.setHospitalId(null);
        invitation.setPharmacyId(pharmacyId);
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(INVITATION_TTL_HOURS));

        Invitation saved = invitationRepository.saveAndFlush(invitation);
        InvitationResponseDto response = invitationMapper.toResponse(saved);

        String invitationLink = buildInvitationLink(saved.getToken());
        String body = emailTemplateBuilder.buildPharmacistInvitationEmail(pharmacy.getName(), invitationLink);
        emailService.sendEmail(new EmailRequest(saved.getEmail(), PHARMACIST_INVITE_SUBJECT, body, true));

        domainEventService.publish(
            "INVITATION_CREATED",
            "INVITATION",
            saved.getId(),
            "PHARMACY",
            pharmacyId,
            Map.of("role", saved.getRole().name(), "email", saved.getEmail())
        );

        return response;
    }

    @Override
    @Transactional
    public Invitation validateToken(String token) {
        Invitation invitation = getByTokenEntity(token);

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Invitation is not in pending state");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            Invitation expired = invitationRepository.save(invitation);
            domainEventService.publish(
                "INVITATION_EXPIRED",
                "INVITATION",
                expired.getId(),
                "PLATFORM",
                null,
                Map.of("token", expired.getToken())
            );
            throw new BadRequestException("Invitation token has expired");
        }

        domainEventService.publish(
            "INVITATION_VALIDATED",
            "INVITATION",
            invitation.getId(),
            "PLATFORM",
            null,
            Map.of("role", invitation.getRole().name())
        );

        return invitation;
    }

    @Override
    @Transactional
    public void markAsAccepted(String token) {
        Invitation invitation = validateToken(token);
        invitation.setStatus(InvitationStatus.ACCEPTED);
        Invitation saved = invitationRepository.save(invitation);
        domainEventService.publish(
            "INVITATION_ACCEPTED",
            "INVITATION",
            saved.getId(),
            "PLATFORM",
            null,
            Map.of("role", saved.getRole().name())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public InvitationResponseDto getByToken(String token) {
        Invitation invitation = getByTokenEntity(token);
        return invitationMapper.toResponse(invitation);
    }

    private Invitation getByTokenEntity(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new BadRequestException("token is required");
        }

        return invitationRepository.findByToken(token.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found for token"));
    }

    private String buildInvitationLink(String token) {
        String baseUrl = invitationBaseUrl == null ? "http://localhost:8080/api/invitations" : invitationBaseUrl.trim();
        if (baseUrl.endsWith("/")) {
            return baseUrl + token;
        }
        return baseUrl + "/" + token;
    }
}
