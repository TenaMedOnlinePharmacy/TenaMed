package com.TenaMed.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AdminPharmacistResponseDto {

    private UUID id;
    private String email;
    private String password;
    private String role;
}
