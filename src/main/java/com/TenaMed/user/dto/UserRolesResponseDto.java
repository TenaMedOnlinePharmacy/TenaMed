package com.TenaMed.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class UserRolesResponseDto {

    private UUID userId;
    private List<String> roles;
}
