package com.TenaMed.user.security;

import com.TenaMed.user.model.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private final String secret;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;
    private SecretKey secretKey;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.access-expiration-ms}") long accessExpirationMs,
                      @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.secret = secret;
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @PostConstruct
    void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, UUID sessionId) {
        return generateAccessToken(userId, sessionId, List.of());
    }

    public String generateAccessToken(UUID userId, UUID sessionId, Collection<String> roles) {
        Map<String, Object> extraClaims = new LinkedHashMap<>();
        if (roles != null && !roles.isEmpty()) {
            extraClaims.put("roles", List.copyOf(roles));
        }
        return generateToken(userId, sessionId, TokenType.ACCESS, accessExpirationMs, extraClaims);
    }

    public String generateRefreshToken(UUID userId, UUID sessionId) {
        return generateToken(userId, sessionId, TokenType.REFRESH, refreshExpirationMs, Map.of());
    }

    public boolean isValidToken(String token, TokenType expectedType) {
        try {
            Claims claims = parseClaims(token);
            String type = claims.get("type", String.class);
            String sid = claims.get("sid", String.class);
            String jti = claims.getId();
            if (type == null || sid == null || jti == null) {
                return false;
            }
            return expectedType.name().equals(type) && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public UUID extractSessionId(String token) {
        return UUID.fromString(parseClaims(token).get("sid", String.class));
    }

    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    public Instant extractExpiration(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    private String generateToken(UUID userId,
                                 UUID sessionId,
                                 TokenType tokenType,
                                 long expirationMs,
                                 Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        JwtBuilder builder = Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .claim("sid", sessionId.toString())
                .claim("type", tokenType.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)));

        if (extraClaims != null && !extraClaims.isEmpty()) {
            extraClaims.forEach(builder::claim);
        }

        return builder.signWith(secretKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        Jws<Claims> jwsClaims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
        return jwsClaims.getPayload();
    }
}
