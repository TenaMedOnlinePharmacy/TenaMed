package com.TenaMed.user.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class RedisSession {

    private UUID sessionId;
    private UUID userId;
    private String accessTokenHash;
    private String refreshTokenHash;
    private SessionStatus status;
    private Instant expiresAt;
    private String deviceInfo;
}
