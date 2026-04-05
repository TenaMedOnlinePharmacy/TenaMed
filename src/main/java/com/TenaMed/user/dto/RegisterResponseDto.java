package com.TenaMed.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class RegisterResponseDto {

    private UUID accountId;
    private UUID userId;
    private String email;
    private String accountStatus;
    private Integer failedLoginAttempts;
    private String firstName;
    private String lastName;
    private String phone;
    private Map<String, Object> address;
    private List<String> roles;
    private LocalDateTime createdAt;
}
