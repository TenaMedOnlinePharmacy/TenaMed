package com.TenaMed.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthTokenResponseDto {

    private String accessToken;
}
