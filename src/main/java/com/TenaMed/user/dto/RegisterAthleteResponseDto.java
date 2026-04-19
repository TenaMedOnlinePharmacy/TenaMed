package com.TenaMed.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class RegisterAthleteResponseDto {

    private RegisterResponseDto athlete;
    private AthleteProfileDto athleteProfile;

    @Data
    @AllArgsConstructor
    public static class AthleteProfileDto {
        private UUID userId;
        private Boolean advisorEnabled;
        private LocalDateTime createdAt;
    }
}
