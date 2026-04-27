package com.TenaMed.user.service;

public interface OtpService {
    void sendOtp(String identifier, String type);
    void verifyOtp(String identifier, String type, String otp);
}
