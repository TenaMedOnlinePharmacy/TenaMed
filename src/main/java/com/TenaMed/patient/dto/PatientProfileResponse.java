package com.TenaMed.patient.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PatientProfileResponse {

    private UUID id;
    private UUID userId;
    private String name;
    private LocalDate dateOfBirth;
    private String gender;
    private Float weight;
    private Integer height;
    private Boolean isPregnant;
    private String bloodType;
    private String uniqueCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AllergyItem> allergies;

    @Data
    @Builder
    public static class AllergyItem {
        private String allergenName;

    }
}
