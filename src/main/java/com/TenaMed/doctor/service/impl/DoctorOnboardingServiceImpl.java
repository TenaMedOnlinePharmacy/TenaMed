package com.TenaMed.doctor.service.impl;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.doctor.dto.DoctorInviteRegistrationRequestDto;
import com.TenaMed.doctor.dto.DoctorResponseDto;
import com.TenaMed.doctor.service.DoctorOnboardingService;
import com.TenaMed.doctor.service.DoctorService;
import com.TenaMed.invitation.entity.Invitation;
import com.TenaMed.invitation.entity.InvitationInstituteType;
import com.TenaMed.invitation.entity.InvitationRole;
import com.TenaMed.invitation.service.InvitationService;
import com.TenaMed.user.dto.RegisterRequestDto;
import com.TenaMed.user.dto.RegisterResponseDto;
import com.TenaMed.user.service.IdentityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class DoctorOnboardingServiceImpl implements DoctorOnboardingService {

    private static final String DOCTOR_ROLE = "DOCTOR";

    private final InvitationService invitationService;
    private final IdentityService identityService;
    private final DoctorService doctorService;

    public DoctorOnboardingServiceImpl(InvitationService invitationService,
                                       IdentityService identityService,
                                       DoctorService doctorService) {
        this.invitationService = invitationService;
        this.identityService = identityService;
        this.doctorService = doctorService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DoctorResponseDto registerAndCreateDoctorFromInvite(String token, DoctorInviteRegistrationRequestDto requestDto) {
        if (requestDto == null || requestDto.getUser() == null || requestDto.getDoctor() == null) {
            throw new BadRequestException("user and doctor payload are required");
        }

        Invitation invitation = invitationService.validateToken(token);
        if (invitation.getRole() != InvitationRole.DOCTOR) {
            throw new BadRequestException("Invitation role does not allow doctor onboarding");
        }
        if (invitation.getInstituteType() != InvitationInstituteType.HOSPITAL || invitation.getInstituteId() == null) {
            throw new BadRequestException("Invitation is not linked to a hospital");
        }

        String invitationEmail = invitation.getEmail() == null ? "" : invitation.getEmail().trim();
        String requestEmail = requestDto.getUser().getEmail() == null ? "" : requestDto.getUser().getEmail().trim();
        if (!invitationEmail.equalsIgnoreCase(requestEmail)) {
            throw new BadRequestException("Invitation email does not match registration email");
        }

        identityService.populateRoles();

        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setEmail(requestDto.getUser().getEmail());
        registerRequest.setPassword(requestDto.getUser().getPassword());
        registerRequest.setFirstName(requestDto.getUser().getFirstName());
        registerRequest.setLastName(requestDto.getUser().getLastName());
        registerRequest.setPhone(requestDto.getUser().getPhone());
        registerRequest.setAddress(requestDto.getUser().getAddress());
        registerRequest.setRoleNames(Set.of(DOCTOR_ROLE));

        RegisterResponseDto registerResponse = identityService.register(registerRequest);

        DoctorResponseDto doctorResponse = doctorService.createDoctorFromInvite(
                registerResponse.getUserId(),
            invitation.getInstituteId(),
                requestDto.getDoctor());

        invitationService.markAsAccepted(token);
        return doctorResponse;
    }
}