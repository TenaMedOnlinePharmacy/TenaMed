package com.TenaMed.common.security;

import com.TenaMed.common.exception.UnauthorizedException;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public UUID getCurrentUserId() {
        Authentication authentication = getAuthentication();

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUserPrincipal authenticatedUserPrincipal) {
            return authenticatedUserPrincipal.getUserId();
        }
        if (principal instanceof UUID userId) {
            return userId;
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (Exception ex) {
            throw new UnauthorizedException("Unable to resolve current user id from authentication context");
        }
    }

    @Override
    public Set<String> getCurrentUserRoles() {
        Authentication authentication = getAuthentication();
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Authentication required");
        }
        return authentication;
    }
}
