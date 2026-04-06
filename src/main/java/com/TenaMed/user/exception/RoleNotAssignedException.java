package com.TenaMed.user.exception;

import java.util.UUID;

public class RoleNotAssignedException extends IdentityException {

    public RoleNotAssignedException(UUID userId, String roleName) {
        super("Role " + roleName + " is not assigned to user " + userId);
    }
}
