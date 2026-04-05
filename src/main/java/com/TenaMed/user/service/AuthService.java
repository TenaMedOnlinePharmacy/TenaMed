package com.TenaMed.user.service;

import com.TenaMed.user.dto.LoginRequestDto;
import com.TenaMed.user.model.AuthTokenPair;

import java.util.UUID;

public interface AuthService {

    AuthTokenPair login(LoginRequestDto requestDto, String deviceInfo);

    AuthTokenPair refresh(String refreshToken, String sessionIdCookie, String deviceInfo);

    void logout(String sessionIdCookie);

    void logoutAll(UUID userId);
}
