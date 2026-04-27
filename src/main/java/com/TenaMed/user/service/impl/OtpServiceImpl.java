package com.TenaMed.user.service.impl;

import com.TenaMed.email.service.EmailService;
import com.TenaMed.user.exception.OtpException;
import com.TenaMed.user.repository.AccountRepository;
import com.TenaMed.user.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final RedisTemplate<String, String> redisTemplate;
    private final AccountRepository accountRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration OTP_EXPIRY = Duration.ofMinutes(5);

    private static final String OTP_KEY_PREFIX = "otp:%s:%s";
    private static final String ATTEMPT_KEY_PREFIX = "otp:attempt:%s:%s";

    @Override
    public void sendOtp(String email, String type) {
        // 1. Check if email exists in account table
        if (!accountRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
            throw new OtpException("Account not found with this email");
        }

        // 2. Generate and store OTP
        String otp = generateOtp();
        String typeKey = type.toLowerCase();
        String otpKey = String.format(OTP_KEY_PREFIX, typeKey, email);
        String attemptKey = String.format(ATTEMPT_KEY_PREFIX, typeKey, email);

        redisTemplate.opsForValue().set(otpKey, otp, OTP_EXPIRY);
        redisTemplate.delete(attemptKey);

        log.info("OTP generated and stored for email: {} and type: {}", email, type);

        // 3. Send email
        emailService.sendOtpEmail(email, otp);
    }

    @Override
    public void verifyOtp(String email, String type, String otp) {
        String typeKey = type.toLowerCase();
        String otpKey = String.format(OTP_KEY_PREFIX, typeKey, email);
        String attemptKey = String.format(ATTEMPT_KEY_PREFIX, typeKey, email);

        String storedOtp = redisTemplate.opsForValue().get(otpKey);
        if (storedOtp == null) {
            throw new OtpException("OTP expired or not found");
        }

        String attemptsStr = redisTemplate.opsForValue().get(attemptKey);
        int attempts = attemptsStr == null ? 0 : Integer.parseInt(attemptsStr);

        if (attempts >= MAX_ATTEMPTS) {
            throw new OtpException("Too many attempts");
        }

        if (!storedOtp.equals(otp)) {
            attempts++;
            Long remainingTtl = redisTemplate.getExpire(otpKey);
            if (remainingTtl != null && remainingTtl > 0) {
                redisTemplate.opsForValue().set(attemptKey, String.valueOf(attempts), Duration.ofSeconds(remainingTtl));
            } else {
                redisTemplate.opsForValue().set(attemptKey, String.valueOf(attempts));
            }
            throw new OtpException("Invalid OTP");
        }

        redisTemplate.delete(otpKey);
        redisTemplate.delete(attemptKey);
        log.info("OTP verified successfully for email: {} and type: {}", email, type);
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }
}
