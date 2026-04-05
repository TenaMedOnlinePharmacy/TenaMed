package com.TenaMed.user.mapper;

import com.TenaMed.user.dto.LoginResponseDto;
import com.TenaMed.user.dto.RegisterResponseDto;
import com.TenaMed.user.entity.Account;
import com.TenaMed.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class IdentityMapper {

    public RegisterResponseDto toRegisterResponse(User user) {
        Account account = user.getAccount();
        return RegisterResponseDto.builder()
                .accountId(account.getId())
                .userId(user.getId())
                .email(account.getEmail())
                .accountStatus(account.getAccountStatus())
                .failedLoginAttempts(account.getFailedLoginAttempts())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .roles(sortedRoleNames(user))
                .createdAt(account.getCreatedAt())
                .build();
    }

    public LoginResponseDto toLoginResponse(User user) {
        Account account = user.getAccount();
        return LoginResponseDto.builder()
                .accountId(account.getId())
                .userId(user.getId())
                .email(account.getEmail())
                .accountStatus(account.getAccountStatus())
                .lastLogin(account.getLastLogin())
                .verifiedAt(account.getVerifiedAt())
                .roles(sortedRoleNames(user))
                .build();
    }

    private List<String> sortedRoleNames(User user) {
        return user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
