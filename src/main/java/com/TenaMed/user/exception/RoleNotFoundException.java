package com.TenaMed.user.exception;

import java.util.Set;

public class RoleNotFoundException extends IdentityException {

    public RoleNotFoundException(Set<String> missingRoles) {
        super("Roles not found: " + String.join(", ", missingRoles));
    }
}
