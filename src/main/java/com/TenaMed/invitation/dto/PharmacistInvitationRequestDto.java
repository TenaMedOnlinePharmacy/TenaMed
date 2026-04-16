package com.TenaMed.invitation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PharmacistInvitationRequestDto {

    @NotBlank
    @Email
    private String email;
}