package com.TenaMed.pharmacy.service.impl;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.invitation.entity.Invitation;
import com.TenaMed.invitation.entity.InvitationInstituteType;
import com.TenaMed.invitation.entity.InvitationRole;
import com.TenaMed.invitation.service.InvitationService;
import com.TenaMed.pharmacy.dto.request.AddStaffRequest;
import com.TenaMed.pharmacy.dto.request.PharmacistInviteRegistrationRequestDto;
import com.TenaMed.pharmacy.dto.response.StaffResponse;
import com.TenaMed.pharmacy.enums.EmploymentStatus;
import com.TenaMed.pharmacy.enums.StaffRole;
import com.TenaMed.pharmacy.service.PharmacistOnboardingService;
import com.TenaMed.pharmacy.service.StaffService;
import com.TenaMed.user.dto.RegisterRequestDto;
import com.TenaMed.user.dto.RegisterResponseDto;
import com.TenaMed.user.service.IdentityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service("pharmacyPharmacistOnboardingService")
public class PharmacistOnboardingServiceImpl implements PharmacistOnboardingService {

    private static final String PHARMACIST_ROLE = "PHARMACIST";

    private final InvitationService invitationService;
    private final IdentityService identityService;
    private final StaffService staffService;

    public PharmacistOnboardingServiceImpl(InvitationService invitationService,
                                           IdentityService identityService,
                                           StaffService staffService) {
        this.invitationService = invitationService;
        this.identityService = identityService;
        this.staffService = staffService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StaffResponse registerAndCreatePharmacistFromInvite(String token, PharmacistInviteRegistrationRequestDto requestDto) {
        if (requestDto == null || requestDto.getUser() == null || requestDto.getPharmacist() == null) {
            throw new BadRequestException("user and pharmacist payload are required");
        }

        Invitation invitation = invitationService.validateToken(token);
        if (invitation.getRole() != InvitationRole.PHARMACIST) {
            throw new BadRequestException("Invitation role does not allow pharmacist onboarding");
        }
        if (invitation.getInstituteType() != InvitationInstituteType.PHARMACY || invitation.getInstituteId() == null) {
            throw new BadRequestException("Invitation is not linked to a pharmacy");
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
        registerRequest.setRoleNames(Set.of(PHARMACIST_ROLE));

        RegisterResponseDto registerResponse = identityService.register(registerRequest);

        AddStaffRequest addStaffRequest = new AddStaffRequest();
        addStaffRequest.setUserId(registerResponse.getUserId());
        addStaffRequest.setPharmacyId(invitation.getInstituteId());
        addStaffRequest.setStaffRole(StaffRole.PHARMACIST);
        addStaffRequest.setEmploymentStatus(EmploymentStatus.ACTIVE);
        addStaffRequest.setLicenseNumber(requestDto.getPharmacist().getLicenseNumber());
        addStaffRequest.setLicenseExpiry(requestDto.getPharmacist().getLicenseExpiry());
        addStaffRequest.setCanVerifyPrescriptions(requestDto.getPharmacist().getCanVerifyPrescriptions());
        addStaffRequest.setCanManageInventory(requestDto.getPharmacist().getCanManageInventory());

        StaffResponse staffResponse = staffService.addStaff(addStaffRequest);
        invitationService.markAsAccepted(token);
        return staffResponse;
    }
}
