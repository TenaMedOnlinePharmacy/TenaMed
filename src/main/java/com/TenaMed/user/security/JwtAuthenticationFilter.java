package com.TenaMed.user.security;

import com.TenaMed.user.model.RedisSession;
import com.TenaMed.user.model.SessionStatus;
import com.TenaMed.user.model.TokenType;
import com.TenaMed.user.service.RedisSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/auth/register-hospital-owner",
            "/api/auth/register-pharmacist",
            "/api/doctors/create",
            "/api/pharmacists/create",
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/identity/login",
            "/api/identity/register",
            "/api/payments/webhook"
    );

    private final JwtService jwtService;
    private final RedisSessionService redisSessionService;
    private final TokenHashingService tokenHashingService;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   RedisSessionService redisSessionService,
                                   TokenHashingService tokenHashingService,
                                   CustomUserDetailsService customUserDetailsService) {
        this.jwtService = jwtService;
        this.redisSessionService = redisSessionService;
        this.tokenHashingService = tokenHashingService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = authHeader.substring(7).trim();
        if (accessToken.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtService.isValidToken(accessToken, TokenType.ACCESS)) {
            unauthorized(response, "Invalid access token");
            return;
        }

        UUID userId = jwtService.extractUserId(accessToken);
        UUID sessionId = jwtService.extractSessionId(accessToken);
        String jti = jwtService.extractJti(accessToken);

        Optional<RedisSession> sessionOpt = redisSessionService.getSession(sessionId);
        if (sessionOpt.isEmpty()) {
            LOGGER.warn("JWT valid but Redis session missing. sid={}, userId={}, jti={}", sessionId, userId, jti);
            unauthorized(response, "Session not found");
            return;
        }

        RedisSession session = sessionOpt.get();
        if (session.getStatus() != SessionStatus.ACTIVE) {
            unauthorized(response, "Session is not active");
            return;
        }

        String incomingTokenHash = tokenHashingService.sha256(accessToken);
        if (!incomingTokenHash.equals(session.getAccessTokenHash())) {
            unauthorized(response, "Access token mismatch");
            return;
        }

        AuthenticatedUserPrincipal principal = customUserDetailsService.loadPrincipalByUserId(userId);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return ALLOWED_PATHS.contains(path)
                || path.startsWith("/api/invitations/")
            || path.startsWith("/api/doctors/create")
            || path.startsWith("/api/pharmacists/create");
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
