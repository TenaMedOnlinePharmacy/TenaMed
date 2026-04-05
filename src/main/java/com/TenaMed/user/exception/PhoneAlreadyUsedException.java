package com.TenaMed.user.exception;

public class PhoneAlreadyUsedException extends IdentityException {

    public PhoneAlreadyUsedException(String phone) {
        super("Phone number is already in use: " + phone);
    }
}
