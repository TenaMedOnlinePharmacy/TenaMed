package com.TenaMed.user.service.impl;

import com.TenaMed.user.dto.LoginRequestDto;
import com.TenaMed.user.entity.Account;
import com.TenaMed.user.exception.InvalidCredentialsException;
import com.TenaMed.user.exception.InvalidSessionException;
import com.TenaMed.user.model.AuthTokenPair;
import com.TenaMed.user.model.RedisSession;
import com.TenaMed.user.model.SessionStatus;
import com.TenaMed.user.model.TokenType;
import com.TenaMed.user.repository.AccountRepository;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import com.TenaMed.user.security.CustomUserDetailsService;
import com.TenaMed.user.security.JwtService;
import com.TenaMed.user.security.TokenHashingService;
import com.TenaMed.user.service.AuthService;
import com.TenaMed.user.service.RedisSessionService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RedisSessionService redisSessionService;
    private final TokenHashingService tokenHashingService;
    private final AccountRepository accountRepository;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           RedisSessionService redisSessionService,
                           TokenHashingService tokenHashingService,
                           AccountRepository accountRepository,
                           CustomUserDetailsService customUserDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.redisSessionService = redisSessionService;
        this.tokenHashingService = tokenHashingService;
        this.accountRepository = accountRepository;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    public AuthTokenPair login(LoginRequestDto requestDto, String deviceInfo) {
        String email = requestDto.getEmail().trim().toLowerCase();
        Account account = accountRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!ACTIVE_STATUS.equalsIgnoreCase(account.getAccountStatus())) {
            throw new InvalidSessionException("Account is not active");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, requestDto.getPassword())
            );
            AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) authentication.getPrincipal();
            UUID sessionId = UUID.randomUUID();

            String accessToken = jwtService.generateAccessToken(principal.getUserId(), sessionId);
            String refreshToken = jwtService.generateRefreshToken(principal.getUserId(), sessionId);

            redisSessionService.createSession(
                    principal.getUserId(),
                    sessionId,
                    tokenHashingService.sha256(accessToken),
                    tokenHashingService.sha256(refreshToken),
                    deviceInfo,
                    jwtService.extractExpiration(refreshToken)
            );

            account.setFailedLoginAttempts(0);
            account.setLastLogin(LocalDateTime.now());
            accountRepository.save(account);

            return AuthTokenPair.builder()
                    .userId(principal.getUserId())
                    .sessionId(sessionId)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        } catch (BadCredentialsException ex) {
            int attempts = account.getFailedLoginAttempts() == null ? 0 : account.getFailedLoginAttempts();
            account.setFailedLoginAttempts(attempts + 1);
            accountRepository.save(account);
            throw new InvalidCredentialsException();
        }
    }

    @Override
    public AuthTokenPair refresh(String refreshToken, String sessionIdCookie, String deviceInfo) {
        if (refreshToken == null || sessionIdCookie == null) {
            throw new InvalidSessionException("Missing refresh token or session cookie");
        }

        if (!jwtService.isValidToken(refreshToken, TokenType.REFRESH)) {
            throw new InvalidSessionException("Invalid refresh token");
        }

        UUID sessionId = parseSessionId(sessionIdCookie);
        UUID userId = jwtService.extractUserId(refreshToken);
        UUID sidFromToken = jwtService.extractSessionId(refreshToken);
        if (!sessionId.equals(sidFromToken)) {
            throw new InvalidSessionException("Session mismatch");
        }

        RedisSession session = redisSessionService.getSession(sessionId)
                .orElseThrow(() -> new InvalidSessionException("Session not found"));

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new InvalidSessionException("Session is not active");
        }

        String incomingRefreshHash = tokenHashingService.sha256(refreshToken);
        if (!incomingRefreshHash.equals(session.getRefreshTokenHash())) {
            throw new InvalidSessionException("Refresh token mismatch");
        }

        customUserDetailsService.loadPrincipalByUserId(userId);
        String newAccessToken = jwtService.generateAccessToken(userId, sessionId);
        String newRefreshToken = jwtService.generateRefreshToken(userId, sessionId);

        boolean rotated = redisSessionService.rotateTokensAtomically(
                sessionId,
                tokenHashingService.sha256(newAccessToken),
                tokenHashingService.sha256(newRefreshToken),
                jwtService.extractExpiration(newRefreshToken)
        );

        if (!rotated) {
            throw new InvalidSessionException("Failed to rotate refresh session");
        }

        return AuthTokenPair.builder()
                .userId(userId)
                .sessionId(sessionId)
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Override
    public void logout(String sessionIdCookie) {
        UUID sessionId = parseSessionId(sessionIdCookie);
        redisSessionService.getSession(sessionId).ifPresent(session -> {
            redisSessionService.deleteSession(sessionId);
            redisSessionService.removeSessionFromUserIndex(session.getUserId(), sessionId);
        });
    }

    @Override
    public void logoutAll(UUID userId) {
        Set<String> sessionIds = redisSessionService.getUserSessionIds(userId);
        for (String sid : sessionIds) {
            redisSessionService.deleteSession(UUID.fromString(sid));
        }
        redisSessionService.deleteUserSessionIndex(userId);
    }

    private UUID parseSessionId(String sessionIdCookie) {
        try {
            return UUID.fromString(sessionIdCookie);
        } catch (IllegalArgumentException ex) {
            throw new InvalidSessionException("Invalid session id");
        }
    }
}
