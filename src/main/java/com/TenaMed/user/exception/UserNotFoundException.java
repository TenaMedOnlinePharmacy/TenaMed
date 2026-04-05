package com.TenaMed.user.exception;

import java.util.UUID;

public class UserNotFoundException extends IdentityException {

    public UserNotFoundException(UUID userId) {
        super("User not found with id: " + userId);
    }
}
