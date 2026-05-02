package com.TenaMed.user.controller;

import com.TenaMed.user.dto.LoginRequestDto;
import com.TenaMed.user.dto.LoginResponseDto;
import com.TenaMed.user.dto.RegisterRequestDto;
import com.TenaMed.user.dto.RegisterResponseDto;
import com.TenaMed.user.dto.AccountInfoResponseDto;
import com.TenaMed.user.dto.UpdateAccountRequestDto;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import com.TenaMed.user.service.IdentityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/identity")
public class IdentityController {

    private final IdentityService identityService;

    public IdentityController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@Valid @RequestBody RegisterRequestDto requestDto) {
        RegisterResponseDto responseDto = identityService.register(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto requestDto) {
        LoginResponseDto responseDto = identityService.login(requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getAccountInfo(Principal principal) {
        UUID userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        return ResponseEntity.ok(identityService.getAccountInfo(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateAccountInfo(Principal principal,
                                               @Valid @RequestBody UpdateAccountRequestDto requestDto) {
        UUID userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        return ResponseEntity.ok(identityService.updateAccountInfo(userId, requestDto));
    }

    private UUID resolveUserId(Principal principal) {
        AuthenticatedUserPrincipal authenticatedUserPrincipal = resolveAuthenticatedPrincipal(principal);
        if (authenticatedUserPrincipal != null) {
            return authenticatedUserPrincipal.getUserId();
        }
        return null;
    }

    private AuthenticatedUserPrincipal resolveAuthenticatedPrincipal(Principal principal) {
        if (principal instanceof AuthenticatedUserPrincipal directPrincipal) {
            return directPrincipal;
        }
        Authentication authentication = resolveAuthentication(principal);
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal nestedPrincipal) {
            return nestedPrincipal;
        }
        return null;
    }

    private Authentication resolveAuthentication(Principal principal) {
        if (principal instanceof Authentication authentication) {
            return authentication;
        }
        Authentication contextAuth = SecurityContextHolder.getContext().getAuthentication();
        if (contextAuth != null && contextAuth.isAuthenticated()) {
            return contextAuth;
        }
        return null;
    }
}
