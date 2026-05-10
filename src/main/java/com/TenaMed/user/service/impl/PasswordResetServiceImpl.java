package com.TenaMed.user.service.impl;

import com.TenaMed.user.exception.OtpException;
import com.TenaMed.user.repository.AccountRepository;
import com.TenaMed.user.service.OtpService;
import com.TenaMed.user.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final String OTP_TYPE = "password_reset";
    private static final String VERIFIED_KEY_PREFIX = "otp:verified:%s:%s";
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(10);

    private final OtpService otpService;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void requestReset(String email) {
        String normalizedEmail = normalizeEmail(email);
        otpService.sendOtp(normalizedEmail, OTP_TYPE);
    }

    @Override
    public void verifyOtp(String email, String otp) {
        String normalizedEmail = normalizeEmail(email);
        otpService.verifyOtp(normalizedEmail, OTP_TYPE, otp);
        String verifiedKey = String.format(VERIFIED_KEY_PREFIX, OTP_TYPE, normalizedEmail);
        redisTemplate.opsForValue().set(verifiedKey, "true", VERIFIED_TTL);
    }

    @Override
    public void resetPassword(String email, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
        String verifiedKey = String.format(VERIFIED_KEY_PREFIX, OTP_TYPE, normalizedEmail);
        String verified = redisTemplate.opsForValue().get(verifiedKey);
        if (verified == null) {
            throw new OtpException("OTP verification required");
        }

        var account = accountRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)
                .orElseThrow(() -> new OtpException("Account not found with this email"));

        account.setPasswordHash(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
        redisTemplate.delete(verifiedKey);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase();
    }
}
