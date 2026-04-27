package com.TenaMed.user.service;

public interface OtpService {
    void sendOtp(String email, String type);
    void verifyOtp(String email, String type, String otp);
}
