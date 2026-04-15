package com.TenaMed.invitation.service.impl;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.common.exception.ResourceNotFoundException;
import com.TenaMed.invitation.dto.InvitationResponseDto;
import com.TenaMed.invitation.entity.Invitation;
import com.TenaMed.invitation.entity.InvitationRole;
import com.TenaMed.invitation.entity.InvitationStatus;
import com.TenaMed.invitation.mapper.InvitationMapper;
import com.TenaMed.invitation.repository.InvitationRepository;
import com.TenaMed.invitation.service.InvitationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InvitationServiceImpl implements InvitationService {

    private static final long INVITATION_TTL_HOURS = 24L;

    private final InvitationRepository invitationRepository;
    private final InvitationMapper invitationMapper;

    public InvitationServiceImpl(InvitationRepository invitationRepository,
                                 InvitationMapper invitationMapper) {
        this.invitationRepository = invitationRepository;
        this.invitationMapper = invitationMapper;
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

        Invitation invitation = new Invitation();
        invitation.setEmail(email.trim().toLowerCase());
        invitation.setRole(InvitationRole.DOCTOR);
        invitation.setHospitalId(hospitalId);
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(INVITATION_TTL_HOURS));

        Invitation saved = invitationRepository.save(invitation);
        return invitationMapper.toResponse(saved);
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
            invitationRepository.save(invitation);
            throw new BadRequestException("Invitation token has expired");
        }

        return invitation;
    }

    @Override
    @Transactional
    public void markAsAccepted(String token) {
        Invitation invitation = validateToken(token);
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);
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
}
