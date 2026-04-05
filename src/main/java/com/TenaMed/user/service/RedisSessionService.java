package com.TenaMed.user.service;

import com.TenaMed.user.model.RedisSession;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface RedisSessionService {

    void createSession(UUID userId,
                       UUID sessionId,
                       String accessTokenHash,
                       String refreshTokenHash,
                       String deviceInfo,
                       Instant expiresAt);

    Optional<RedisSession> getSession(UUID sessionId);

    boolean rotateTokensAtomically(UUID sessionId,
                                   String newAccessTokenHash,
                                   String newRefreshTokenHash,
                                   Instant newExpiresAt);

    void deleteSession(UUID sessionId);

    Set<String> getUserSessionIds(UUID userId);

    void removeSessionFromUserIndex(UUID userId, UUID sessionId);

    void deleteUserSessionIndex(UUID userId);
}
