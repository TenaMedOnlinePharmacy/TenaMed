package com.TenaMed.patient.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateProfileDto {
    private LocalDate dateOfBirth;
    private String gender;
    private Float weight;
    private Integer height;
    private Boolean isPregnant;
    private String bloodType;
    private String uniqueCode;
}