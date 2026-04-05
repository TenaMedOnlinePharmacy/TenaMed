package com.TenaMed.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class UserDetailsResponseDto {

    private UUID userId;
    private UUID accountId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Map<String, Object> address;
    private List<String> roles;
}
