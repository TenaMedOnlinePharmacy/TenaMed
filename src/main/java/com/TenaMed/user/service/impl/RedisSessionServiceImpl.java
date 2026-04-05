package com.TenaMed.user.service.impl;

import com.TenaMed.user.model.RedisSession;
import com.TenaMed.user.model.SessionStatus;
import com.TenaMed.user.service.RedisSessionService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class RedisSessionServiceImpl implements RedisSessionService {

    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user_sessions:";

    private static final String USER_ID_FIELD = "userId";
    private static final String ACCESS_TOKEN_HASH_FIELD = "accessTokenHash";
    private static final String REFRESH_TOKEN_HASH_FIELD = "refreshTokenHash";
    private static final String STATUS_FIELD = "status";
    private static final String EXPIRES_AT_FIELD = "expiresAt";
    private static final String DEVICE_INFO_FIELD = "deviceInfo";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisSessionServiceImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void createSession(UUID userId,
                              UUID sessionId,
                              String accessTokenHash,
                              String refreshTokenHash,
                              String deviceInfo,
                              Instant expiresAt) {
        String sessionKey = sessionKey(sessionId);
        String userSessionsKey = userSessionsKey(userId);

        Map<String, String> sessionFields = new HashMap<>();
        sessionFields.put(USER_ID_FIELD, userId.toString());
        sessionFields.put(ACCESS_TOKEN_HASH_FIELD, accessTokenHash);
        sessionFields.put(REFRESH_TOKEN_HASH_FIELD, refreshTokenHash);
        sessionFields.put(STATUS_FIELD, SessionStatus.ACTIVE.name());
        sessionFields.put(EXPIRES_AT_FIELD, expiresAt.toString());
        sessionFields.put(DEVICE_INFO_FIELD, deviceInfo == null ? "UNKNOWN" : deviceInfo);

        redisTemplate.opsForHash().putAll(sessionKey, sessionFields);
        redisTemplate.expire(sessionKey, ttlFromExpiration(expiresAt));
        redisTemplate.opsForSet().add(userSessionsKey, sessionId.toString());
    }

    @Override
    public Optional<RedisSession> getSession(UUID sessionId) {
        String sessionKey = sessionKey(sessionId);
        Map<Object, Object> fields = redisTemplate.opsForHash().entries(sessionKey);
        if (fields == null || fields.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(RedisSession.builder()
                .sessionId(sessionId)
                .userId(UUID.fromString((String) fields.get(USER_ID_FIELD)))
                .accessTokenHash((String) fields.get(ACCESS_TOKEN_HASH_FIELD))
                .refreshTokenHash((String) fields.get(REFRESH_TOKEN_HASH_FIELD))
                .status(SessionStatus.valueOf((String) fields.get(STATUS_FIELD)))
                .expiresAt(Instant.parse((String) fields.get(EXPIRES_AT_FIELD)))
                .deviceInfo((String) fields.get(DEVICE_INFO_FIELD))
                .build());
    }

    @Override
    public boolean rotateTokensAtomically(UUID sessionId,
                                          String newAccessTokenHash,
                                          String newRefreshTokenHash,
                                          Instant newExpiresAt) {
        String sessionKey = sessionKey(sessionId);

        SessionCallback<Boolean> callback = new SessionCallback<>() {
            @Override
            @SuppressWarnings("unchecked")
            public Boolean execute(RedisOperations operations) throws DataAccessException {
                operations.watch(sessionKey);
                Object status = operations.opsForHash().get(sessionKey, STATUS_FIELD);
                if (status == null || !SessionStatus.ACTIVE.name().equals(status.toString())) {
                    operations.unwatch();
                    return false;
                }

                operations.multi();
                operations.opsForHash().put(sessionKey, ACCESS_TOKEN_HASH_FIELD, newAccessTokenHash);
                operations.opsForHash().put(sessionKey, REFRESH_TOKEN_HASH_FIELD, newRefreshTokenHash);
                operations.opsForHash().put(sessionKey, EXPIRES_AT_FIELD, newExpiresAt.toString());
                operations.expire(sessionKey, ttlFromExpiration(newExpiresAt));
                return operations.exec() != null;
            }
        };

        Boolean result = redisTemplate.execute(callback);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public void deleteSession(UUID sessionId) {
        redisTemplate.delete(sessionKey(sessionId));
    }

    @Override
    public Set<String> getUserSessionIds(UUID userId) {
        Set<String> members = redisTemplate.opsForSet().members(userSessionsKey(userId));
        return members == null ? Set.of() : members;
    }

    @Override
    public void removeSessionFromUserIndex(UUID userId, UUID sessionId) {
        redisTemplate.opsForSet().remove(userSessionsKey(userId), sessionId.toString());
    }

    @Override
    public void deleteUserSessionIndex(UUID userId) {
        redisTemplate.delete(userSessionsKey(userId));
    }

    private String sessionKey(UUID sessionId) {
        return SESSION_PREFIX + sessionId;
    }

    private String userSessionsKey(UUID userId) {
        return USER_SESSIONS_PREFIX + userId;
    }

    private Duration ttlFromExpiration(Instant expiresAt) {
        Duration duration = Duration.between(Instant.now(), expiresAt);
        return duration.isNegative() ? Duration.ZERO : duration;
    }
}
