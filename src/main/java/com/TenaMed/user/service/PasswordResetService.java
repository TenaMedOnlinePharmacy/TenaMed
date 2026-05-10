package com.TenaMed.user.service;

public interface PasswordResetService {
    void requestReset(String email);
    void verifyOtp(String email, String otp);
    void resetPassword(String email, String newPassword);
}
