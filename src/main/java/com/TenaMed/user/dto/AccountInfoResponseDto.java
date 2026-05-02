package com.TenaMed.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class AccountInfoResponseDto {
    private String fullName;
    private String email;
    private String phone;
    private Map<String, Object> address;
}
