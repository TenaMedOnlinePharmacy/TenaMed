package com.TenaMed.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSendRequestDto {
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "OTP type is required")
    private String type;
}
