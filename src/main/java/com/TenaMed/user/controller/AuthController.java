package com.TenaMed.user.controller;

import com.TenaMed.user.config.AuthCookieProperties;
import com.TenaMed.user.dto.AuthTokenResponseDto;
import com.TenaMed.user.dto.LoginRequestDto;
import com.TenaMed.user.dto.RegisterRequestDto;
import com.TenaMed.user.dto.RegisterResponseDto;
import com.TenaMed.user.model.AuthTokenPair;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import com.TenaMed.user.service.AuthService;
import com.TenaMed.user.service.IdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IdentityService identityService;
    private final AuthService authService;

    public AuthController(IdentityService identityService, AuthService authService) {
        this.identityService = identityService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@Valid @RequestBody RegisterRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(identityService.register(requestDto));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponseDto> login(@Valid @RequestBody LoginRequestDto requestDto,
                                                      HttpServletRequest request) {
        AuthTokenPair tokenPair = authService.login(requestDto, request.getHeader("User-Agent"));
        return ResponseEntity.ok()
                .header("Set-Cookie", buildRefreshCookie(tokenPair.getRefreshToken()).toString())
                .header("Set-Cookie", buildSessionCookie(tokenPair.getSessionId()).toString())
                .body(AuthTokenResponseDto.builder().accessToken(tokenPair.getAccessToken()).build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponseDto> refresh(
            @CookieValue(name = AuthCookieProperties.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            @CookieValue(name = AuthCookieProperties.SESSION_ID_COOKIE, required = false) String sessionId,
            HttpServletRequest request) {
        AuthTokenPair tokenPair = authService.refresh(refreshToken, sessionId, request.getHeader("User-Agent"));
        return ResponseEntity.ok()
                .header("Set-Cookie", buildRefreshCookie(tokenPair.getRefreshToken()).toString())
                .header("Set-Cookie", buildSessionCookie(tokenPair.getSessionId()).toString())
                .body(AuthTokenResponseDto.builder().accessToken(tokenPair.getAccessToken()).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = AuthCookieProperties.SESSION_ID_COOKIE, required = false) String sessionId) {
        authService.logout(sessionId);
        return ResponseEntity.noContent()
                .header("Set-Cookie", clearCookie(AuthCookieProperties.REFRESH_TOKEN_COOKIE).toString())
                .header("Set-Cookie", clearCookie(AuthCookieProperties.SESSION_ID_COOKIE).toString())
                .build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        authService.logoutAll(principal.getUserId());
        return ResponseEntity.noContent()
                .header("Set-Cookie", clearCookie(AuthCookieProperties.REFRESH_TOKEN_COOKIE).toString())
                .header("Set-Cookie", clearCookie(AuthCookieProperties.SESSION_ID_COOKIE).toString())
                .build();
    }

    private ResponseCookie buildRefreshCookie(String refreshToken) {
        return ResponseCookie.from(AuthCookieProperties.REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(AuthCookieProperties.COOKIE_PATH)
                .maxAge(30L * 24 * 60 * 60)
                .build();
    }

    private ResponseCookie buildSessionCookie(UUID sessionId) {
        return ResponseCookie.from(AuthCookieProperties.SESSION_ID_COOKIE, sessionId.toString())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(AuthCookieProperties.COOKIE_PATH)
                .maxAge(30L * 24 * 60 * 60)
                .build();
    }

    private ResponseCookie clearCookie(String cookieName) {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(AuthCookieProperties.COOKIE_PATH)
                .maxAge(0)
                .build();
    }
}
