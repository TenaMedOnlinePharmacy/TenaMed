package com.TenaMed.user.exception;

public class AccountNotActiveException extends IdentityException {

    public AccountNotActiveException(String accountStatus) {
        super("Account is not active. Current status: " + accountStatus);
    }
}
