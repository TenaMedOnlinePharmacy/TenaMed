package com.TenaMed.invitation.service;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.invitation.dto.InvitationResponseDto;
import com.TenaMed.invitation.entity.Invitation;
import com.TenaMed.invitation.entity.InvitationRole;
import com.TenaMed.invitation.entity.InvitationStatus;
import com.TenaMed.invitation.mapper.InvitationMapper;
import com.TenaMed.invitation.repository.InvitationRepository;
import com.TenaMed.invitation.service.impl.InvitationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceImplTests {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private InvitationMapper invitationMapper;

    @InjectMocks
    private InvitationServiceImpl invitationService;

    @Test
    void shouldCreateDoctorInvitationWithNormalizedEmailAndDefaults() {
        UUID hospitalId = UUID.randomUUID();
        String email = "  DOCTOR@Example.com  ";

        Invitation saved = new Invitation();
        saved.setId(UUID.randomUUID());
        saved.setEmail("doctor@example.com");
        saved.setHospitalId(hospitalId);
        saved.setRole(InvitationRole.DOCTOR);
        saved.setStatus(InvitationStatus.PENDING);
        saved.setToken(UUID.randomUUID().toString());
        saved.setExpiresAt(LocalDateTime.now().plusHours(24));

        InvitationResponseDto response = new InvitationResponseDto();
        response.setId(saved.getId());
        response.setEmail(saved.getEmail());
        response.setHospitalId(hospitalId);
        response.setRole(InvitationRole.DOCTOR);
        response.setStatus(InvitationStatus.PENDING);

        when(invitationRepository.save(any(Invitation.class))).thenReturn(saved);
        when(invitationMapper.toResponse(saved)).thenReturn(response);

        InvitationResponseDto actual = invitationService.createDoctorInvitation(hospitalId, email);

        ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository).save(captor.capture());
        Invitation persisted = captor.getValue();

        assertEquals("doctor@example.com", persisted.getEmail());
        assertEquals(InvitationRole.DOCTOR, persisted.getRole());
        assertEquals(InvitationStatus.PENDING, persisted.getStatus());
        assertNotNull(persisted.getToken());
        assertEquals(hospitalId, actual.getHospitalId());
    }

    @Test
    void shouldExpireInvitationWhenTokenIsExpired() {
        String token = UUID.randomUUID().toString();

        Invitation invitation = new Invitation();
        invitation.setToken(token);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> invitationService.validateToken(token));

        assertTrue(ex.getMessage().contains("expired"));
        assertEquals(InvitationStatus.EXPIRED, invitation.getStatus());
        verify(invitationRepository).save(invitation);
    }

    @Test
    void shouldMarkInvitationAsAcceptedForValidToken() {
        String token = UUID.randomUUID().toString();

        Invitation invitation = new Invitation();
        invitation.setToken(token);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        invitationService.markAsAccepted(token);

        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());
        verify(invitationRepository).save(invitation);
    }

    @Test
    void shouldRejectTokenValidationWhenStatusIsNotPending() {
        String token = UUID.randomUUID().toString();

        Invitation invitation = new Invitation();
        invitation.setToken(token);
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> invitationService.validateToken(token));

        assertTrue(ex.getMessage().contains("pending state"));
    }
}
