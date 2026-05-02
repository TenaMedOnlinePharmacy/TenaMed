package com.TenaMed.patient.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateProfileDto {
    private String name;
    private LocalDate dateOfBirth;
    private String gender;
    private Float weight;
    private Integer height;
    private Boolean isPregnant;
    private String bloodType;
    private String uniqueCode;
    private List<String> allergens;
}