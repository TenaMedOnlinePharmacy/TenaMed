package com.TenaMed.common.security;

import java.util.Set;
import java.util.UUID;

public interface CurrentUserProvider {

    UUID getCurrentUserId();

    Set<String> getCurrentUserRoles();

    default boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }

        Set<String> roles = getCurrentUserRoles();
        String normalized = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return roles.contains(role) || roles.contains(normalized);
    }
}
