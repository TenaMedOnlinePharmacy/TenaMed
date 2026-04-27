package com.TenaMed.user.controller;

import com.TenaMed.user.dto.OtpSendRequestDto;
import com.TenaMed.user.dto.OtpVerifyRequestDto;
import com.TenaMed.user.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendOtp(@Valid @RequestBody OtpSendRequestDto request) {
        otpService.sendOtp(request.getEmail(), request.getType());
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyOtp(@Valid @RequestBody OtpVerifyRequestDto request) {
        otpService.verifyOtp(request.getEmail(), request.getType(), request.getOtp());
        return ResponseEntity.ok(Map.of("message", "OTP verified successfully"));
    }
}
