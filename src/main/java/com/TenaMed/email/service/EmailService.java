package com.TenaMed.email.service;

import com.TenaMed.email.dto.EmailRequest;

public interface EmailService {

    void sendEmail(EmailRequest request);
    void sendOtpEmail(String to, String otp);
}
