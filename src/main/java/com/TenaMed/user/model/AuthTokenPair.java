package com.TenaMed.user.model;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AuthTokenPair {

    private UUID userId;
    private UUID sessionId;
    private String accessToken;
    private String refreshToken;
}
