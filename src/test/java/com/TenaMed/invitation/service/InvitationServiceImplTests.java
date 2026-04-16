package com.TenaMed.invitation.service;

import com.TenaMed.common.exception.BadRequestException;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceImplTests {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private InvitationMapper invitationMapper;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private PharmacyRepository pharmacyRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailTemplateBuilder emailTemplateBuilder;

    @InjectMocks
    private InvitationServiceImpl invitationService;

    @Test
    void shouldCreateDoctorInvitationWithNormalizedEmailAndDefaults() {
        UUID hospitalId = UUID.randomUUID();
        String email = "  DOCTOR@Example.com  ";

        Hospital hospital = new Hospital();
        hospital.setId(hospitalId);
        hospital.setName("Saint Gabriel");

        Invitation saved = new Invitation();
        saved.setId(UUID.randomUUID());
        saved.setEmail("doctor@example.com");
        saved.setHospitalId(hospitalId);
        saved.setInstituteId(hospitalId);
        saved.setInstituteType(InvitationInstituteType.HOSPITAL);
        saved.setRole(InvitationRole.DOCTOR);
        saved.setStatus(InvitationStatus.PENDING);
        saved.setToken(UUID.randomUUID().toString());
        saved.setExpiresAt(LocalDateTime.now().plusHours(24));

        InvitationResponseDto response = new InvitationResponseDto();
        response.setId(saved.getId());
        response.setEmail(saved.getEmail());
        response.setHospitalId(hospitalId);
        response.setInstituteId(hospitalId);
        response.setInstituteType(InvitationInstituteType.HOSPITAL);
        response.setRole(InvitationRole.DOCTOR);
        response.setStatus(InvitationStatus.PENDING);

        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
        when(invitationRepository.saveAndFlush(any(Invitation.class))).thenReturn(saved);
        when(emailTemplateBuilder.buildDoctorInvitationEmail(eq("Saint Gabriel"), contains(saved.getToken())))
            .thenReturn("<html>Invite</html>");
        when(invitationMapper.toResponse(saved)).thenReturn(response);

        InvitationResponseDto actual = invitationService.createDoctorInvitation(hospitalId, email);

        ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository).saveAndFlush(captor.capture());
        Invitation persisted = captor.getValue();

        assertEquals("doctor@example.com", persisted.getEmail());
        assertEquals(InvitationRole.DOCTOR, persisted.getRole());
        assertEquals(InvitationStatus.PENDING, persisted.getStatus());
        assertEquals(hospitalId, persisted.getInstituteId());
        assertEquals(InvitationInstituteType.HOSPITAL, persisted.getInstituteType());
        assertEquals(hospitalId, persisted.getHospitalId());
        assertNull(persisted.getPharmacyId());
        assertNotNull(persisted.getToken());
        assertEquals(hospitalId, actual.getHospitalId());
        verify(emailService).sendEmail(any());
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

    @Test
    void shouldCreatePharmacistInvitationWithPharmacyId() {
        UUID pharmacyId = UUID.randomUUID();

        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setId(pharmacyId);
        pharmacy.setName("City Pharmacy");

        Invitation saved = new Invitation();
        saved.setId(UUID.randomUUID());
        saved.setEmail("pharmacist@example.com");
        saved.setPharmacyId(pharmacyId);
        saved.setInstituteId(pharmacyId);
        saved.setInstituteType(InvitationInstituteType.PHARMACY);
        saved.setRole(InvitationRole.PHARMACIST);
        saved.setStatus(InvitationStatus.PENDING);
        saved.setToken(UUID.randomUUID().toString());
        saved.setExpiresAt(LocalDateTime.now().plusHours(24));

        InvitationResponseDto response = new InvitationResponseDto();
        response.setId(saved.getId());
        response.setEmail(saved.getEmail());
        response.setPharmacyId(pharmacyId);
        response.setInstituteId(pharmacyId);
        response.setInstituteType(InvitationInstituteType.PHARMACY);
        response.setRole(InvitationRole.PHARMACIST);
        response.setStatus(InvitationStatus.PENDING);

        when(pharmacyRepository.findById(pharmacyId)).thenReturn(Optional.of(pharmacy));
        when(invitationRepository.saveAndFlush(any(Invitation.class))).thenReturn(saved);
        when(emailTemplateBuilder.buildPharmacistInvitationEmail(eq("City Pharmacy"), contains(saved.getToken())))
                .thenReturn("<html>Invite</html>");
        when(invitationMapper.toResponse(saved)).thenReturn(response);

        InvitationResponseDto actual = invitationService.createPharmacistInvitation(pharmacyId, "  PHARMACIST@example.com ");

        ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository).saveAndFlush(captor.capture());
        Invitation persisted = captor.getValue();

        assertEquals("pharmacist@example.com", persisted.getEmail());
        assertEquals(InvitationRole.PHARMACIST, persisted.getRole());
        assertNull(persisted.getHospitalId());
        assertEquals(pharmacyId, persisted.getPharmacyId());
        assertEquals(pharmacyId, persisted.getInstituteId());
        assertEquals(InvitationInstituteType.PHARMACY, persisted.getInstituteType());
        assertEquals(pharmacyId, actual.getPharmacyId());
        verify(emailService).sendEmail(any());
    }

    @Test
    void shouldThrowWhenEmailDeliveryFailsDuringInvitationCreation() {
        UUID hospitalId = UUID.randomUUID();
        String email = "doctor@example.com";

        Hospital hospital = new Hospital();
        hospital.setId(hospitalId);
        hospital.setName("Saint Gabriel");

        Invitation saved = new Invitation();
        saved.setId(UUID.randomUUID());
        saved.setEmail(email);
        saved.setHospitalId(hospitalId);
        saved.setRole(InvitationRole.DOCTOR);
        saved.setStatus(InvitationStatus.PENDING);
        saved.setToken(UUID.randomUUID().toString());
        saved.setExpiresAt(LocalDateTime.now().plusHours(24));

        when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
        when(invitationRepository.saveAndFlush(any(Invitation.class))).thenReturn(saved);
        when(emailTemplateBuilder.buildDoctorInvitationEmail(eq("Saint Gabriel"), contains(saved.getToken())))
            .thenReturn("<html>Invite</html>");
        org.mockito.Mockito.doThrow(new IllegalStateException("smtp down"))
            .when(emailService)
            .sendEmail(any());

        assertThrows(IllegalStateException.class,
            () -> invitationService.createDoctorInvitation(hospitalId, email));

        verify(invitationMapper).toResponse(any(Invitation.class));
    }
}
