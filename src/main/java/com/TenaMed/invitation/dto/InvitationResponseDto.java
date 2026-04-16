package com.TenaMed.invitation.dto;

import com.TenaMed.invitation.entity.InvitationRole;
import com.TenaMed.invitation.entity.InvitationStatus;
import com.TenaMed.invitation.entity.InvitationInstituteType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class InvitationResponseDto {
    private UUID id;
    private String email;
    private InvitationRole role;
    private UUID instituteId;
    private InvitationInstituteType instituteType;
    private UUID hospitalId;
    private UUID pharmacyId;
    private String token;
    private InvitationStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
