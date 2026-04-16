package com.TenaMed.invitation.service;

import com.TenaMed.invitation.dto.InvitationResponseDto;
import com.TenaMed.invitation.entity.Invitation;

import java.util.UUID;

public interface InvitationService {

    InvitationResponseDto createDoctorInvitation(UUID hospitalId, String email);

    InvitationResponseDto createPharmacistInvitation(UUID pharmacyId, String email);

    Invitation validateToken(String token);

    void markAsAccepted(String token);

    InvitationResponseDto getByToken(String token);
}
