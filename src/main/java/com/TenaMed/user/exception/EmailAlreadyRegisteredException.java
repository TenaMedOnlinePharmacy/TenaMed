package com.TenaMed.user.exception;

public class EmailAlreadyRegisteredException extends IdentityException {

    public EmailAlreadyRegisteredException(String email) {
        super("Account already exists for email: " + email);
    }
}
