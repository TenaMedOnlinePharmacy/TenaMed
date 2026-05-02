package com.TenaMed.user.service;

import com.TenaMed.user.dto.LoginRequestDto;
import com.TenaMed.user.dto.LoginResponseDto;
import com.TenaMed.user.dto.RegisterRequestDto;
import com.TenaMed.user.dto.RegisterResponseDto;
import com.TenaMed.user.dto.UserDetailsResponseDto;
import com.TenaMed.user.dto.UserRolesResponseDto;
import com.TenaMed.user.dto.AccountInfoResponseDto;
import com.TenaMed.user.dto.UpdateAccountRequestDto;

import java.util.List;
import java.util.UUID;

public interface IdentityService {

    RegisterResponseDto register(RegisterRequestDto requestDto);

    LoginResponseDto login(LoginRequestDto requestDto);

    UserDetailsResponseDto getUserDetails(UUID userId);

    AccountInfoResponseDto getAccountInfo(UUID userId);

    AccountInfoResponseDto updateAccountInfo(UUID userId, UpdateAccountRequestDto requestDto);

    UserRolesResponseDto assignRoleToUser(UUID userId, String roleName);

    UserRolesResponseDto removeRoleFromUser(UUID userId, String roleName);

    UserRolesResponseDto getUserRoles(UUID userId);

    List<String> populateRoles();
}
