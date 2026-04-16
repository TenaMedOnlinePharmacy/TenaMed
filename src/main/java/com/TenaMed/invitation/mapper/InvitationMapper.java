package com.TenaMed.invitation.mapper;

import com.TenaMed.invitation.dto.InvitationResponseDto;
import com.TenaMed.invitation.entity.Invitation;
import org.springframework.stereotype.Component;

@Component
public class InvitationMapper {

    public InvitationResponseDto toResponse(Invitation invitation) {
        InvitationResponseDto dto = new InvitationResponseDto();
        dto.setId(invitation.getId());
        dto.setEmail(invitation.getEmail());
        dto.setRole(invitation.getRole());
        dto.setInstituteId(invitation.getInstituteId());
        dto.setInstituteType(invitation.getInstituteType());
        dto.setHospitalId(invitation.getHospitalId());
        dto.setPharmacyId(invitation.getPharmacyId());
        dto.setToken(invitation.getToken());
        dto.setStatus(invitation.getStatus());
        dto.setExpiresAt(invitation.getExpiresAt());
        dto.setCreatedAt(invitation.getCreatedAt());
        return dto;
    }
}
