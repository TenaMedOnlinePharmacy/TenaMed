package com.TenaMed.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class LoginResponseDto {

    private UUID accountId;
    private UUID userId;
    private String email;
    private String accountStatus;
    private LocalDateTime lastLogin;
    private LocalDateTime verifiedAt;
    private List<String> roles;
}
