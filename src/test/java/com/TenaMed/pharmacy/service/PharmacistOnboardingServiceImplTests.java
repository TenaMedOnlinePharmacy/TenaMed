package com.TenaMed.pharmacy.service;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.invitation.entity.Invitation;
import com.TenaMed.invitation.entity.InvitationInstituteType;
import com.TenaMed.invitation.entity.InvitationRole;
import com.TenaMed.invitation.entity.InvitationStatus;
import com.TenaMed.invitation.service.InvitationService;
import com.TenaMed.pharmacy.dto.request.PharmacistInviteRegistrationRequestDto;
import com.TenaMed.pharmacy.dto.response.StaffResponse;
import com.TenaMed.pharmacy.service.impl.PharmacistOnboardingServiceImpl;
import com.TenaMed.user.dto.RegisterRequestDto;
import com.TenaMed.user.dto.RegisterResponseDto;
import com.TenaMed.user.service.IdentityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PharmacistOnboardingServiceImplTests {

    @Mock
    private InvitationService invitationService;

    @Mock
    private IdentityService identityService;

    @Mock
    private StaffService staffService;

    @InjectMocks
    private PharmacistOnboardingServiceImpl onboardingService;

    @Test
    void shouldRegisterUserThenCreateUserPharmacyFromInvite() {
        UUID pharmacyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Invitation invitation = new Invitation();
        invitation.setEmail("pharmacist@example.com");
        invitation.setRole(InvitationRole.PHARMACIST);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setPharmacyId(pharmacyId);
        invitation.setInstituteId(pharmacyId);
        invitation.setInstituteType(InvitationInstituteType.PHARMACY);

        PharmacistInviteRegistrationRequestDto request = new PharmacistInviteRegistrationRequestDto();
        PharmacistInviteRegistrationRequestDto.UserDto user = new PharmacistInviteRegistrationRequestDto.UserDto();
        user.setEmail("pharmacist@example.com");
        user.setPassword("StrongPass123");
        user.setFirstName("Sami");
        user.setLastName("Ali");
        request.setUser(user);
        request.setPharmacist(new PharmacistInviteRegistrationRequestDto.PharmacistDto());

        RegisterResponseDto registerResponse = RegisterResponseDto.builder().userId(userId).build();
        StaffResponse staffResponse = StaffResponse.builder().userId(userId).pharmacyId(pharmacyId).build();

        when(invitationService.validateToken("token")).thenReturn(invitation);
        when(identityService.register(any(RegisterRequestDto.class))).thenReturn(registerResponse);
        when(staffService.addStaff(any())).thenReturn(staffResponse);

        StaffResponse actual = onboardingService.registerAndCreatePharmacistFromInvite("token", request);

        ArgumentCaptor<RegisterRequestDto> registerCaptor = ArgumentCaptor.forClass(RegisterRequestDto.class);
        verify(identityService).register(registerCaptor.capture());
        assertEquals(Set.of("PHARMACIST"), registerCaptor.getValue().getRoleNames());

        assertEquals(userId, actual.getUserId());
        assertEquals(pharmacyId, actual.getPharmacyId());
        verify(invitationService).markAsAccepted("token");
    }

    @Test
    void shouldRejectWhenInvitationEmailMismatchesRegistrationEmail() {
        Invitation invitation = new Invitation();
        invitation.setEmail("invitee@example.com");
        invitation.setRole(InvitationRole.PHARMACIST);
        invitation.setPharmacyId(UUID.randomUUID());
        invitation.setInstituteId(invitation.getPharmacyId());
        invitation.setInstituteType(InvitationInstituteType.PHARMACY);

        PharmacistInviteRegistrationRequestDto request = new PharmacistInviteRegistrationRequestDto();
        PharmacistInviteRegistrationRequestDto.UserDto user = new PharmacistInviteRegistrationRequestDto.UserDto();
        user.setEmail("other@example.com");
        user.setPassword("StrongPass123");
        user.setFirstName("Sami");
        user.setLastName("Ali");
        request.setUser(user);
        request.setPharmacist(new PharmacistInviteRegistrationRequestDto.PharmacistDto());

        when(invitationService.validateToken("token")).thenReturn(invitation);

        assertThrows(BadRequestException.class,
                () -> onboardingService.registerAndCreatePharmacistFromInvite("token", request));
    }
}
