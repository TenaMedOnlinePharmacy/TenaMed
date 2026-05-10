package com.TenaMed.user.controller;

import com.TenaMed.user.dto.PasswordResetRequestDto;
import com.TenaMed.user.dto.PasswordResetUpdateRequestDto;
import com.TenaMed.user.dto.PasswordResetVerifyRequestDto;
import com.TenaMed.user.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot")
    public ResponseEntity<Map<String, String>> requestReset(@Valid @RequestBody PasswordResetRequestDto request) {
        passwordResetService.requestReset(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyOtp(@Valid @RequestBody PasswordResetVerifyRequestDto request) {
        passwordResetService.verifyOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(Map.of("message", "OTP verified successfully"));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody PasswordResetUpdateRequestDto request) {
        passwordResetService.resetPassword(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}
