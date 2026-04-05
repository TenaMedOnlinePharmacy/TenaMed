package com.TenaMed.user.exception;

import java.util.UUID;

public class RoleAlreadyAssignedException extends IdentityException {

    public RoleAlreadyAssignedException(UUID userId, String roleName) {
        super("Role " + roleName + " is already assigned to user " + userId);
    }
}
