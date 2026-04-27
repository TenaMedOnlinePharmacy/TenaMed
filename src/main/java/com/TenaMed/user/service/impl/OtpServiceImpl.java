package com.TenaMed.user.service.impl;

import com.TenaMed.user.exception.OtpException;
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
    private final SecureRandom secureRandom = new SecureRandom();

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration OTP_EXPIRY = Duration.ofMinutes(5);

    private static final String OTP_KEY_PREFIX = "otp:%s:%s";
    private static final String ATTEMPT_KEY_PREFIX = "otp:attempt:%s:%s";

    @Override
    public void sendOtp(String identifier, String type) {
        String otp = generateOtp();
        String typeKey = type.toLowerCase();
        String otpKey = String.format(OTP_KEY_PREFIX, typeKey, identifier);
        String attemptKey = String.format(ATTEMPT_KEY_PREFIX, typeKey, identifier);

        // Store OTP with 5 min expiry
        redisTemplate.opsForValue().set(otpKey, otp, OTP_EXPIRY);
        
        // Reset attempts on send
        redisTemplate.delete(attemptKey);

        // Requirement 6: Do not expose OTP in production logs
        log.info("OTP generated and stored for identifier: {} and type: {}", identifier, type);
        
        // Note: Actual sending logic (Email/SMS) should be integrated here or called by the controller
    }

    @Override
    public void verifyOtp(String identifier, String type, String otp) {
        String typeKey = type.toLowerCase();
        String otpKey = String.format(OTP_KEY_PREFIX, typeKey, identifier);
        String attemptKey = String.format(ATTEMPT_KEY_PREFIX, typeKey, identifier);

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
            
            // Keep same TTL for attempts as the OTP key
            Long remainingTtl = redisTemplate.getExpire(otpKey);
            if (remainingTtl != null && remainingTtl > 0) {
                redisTemplate.opsForValue().set(attemptKey, String.valueOf(attempts), Duration.ofSeconds(remainingTtl));
            } else {
                redisTemplate.opsForValue().set(attemptKey, String.valueOf(attempts));
            }
            
            throw new OtpException("Invalid OTP");
        }

        // If valid, delete both keys
        redisTemplate.delete(otpKey);
        redisTemplate.delete(attemptKey);
        log.info("OTP verified successfully for identifier: {} and type: {}", identifier, type);
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }
}
