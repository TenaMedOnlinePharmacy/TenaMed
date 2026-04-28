package com.TenaMed.doctor.dto;

import com.TenaMed.prescription.entity.PrescriptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CreateDoctorPrescriptionRequestDto {

    @NotNull
    private LocalDate expiryDate;

    @NotNull
    private Integer maxRefillsAllowed;

    @NotNull
    private Boolean highRisk;

    @NotNull
    private PrescriptionType type;

    @NotBlank
    private String fullName;

    @NotBlank
    private String phone;

    private String uniqueCode;

    @NotEmpty
    private List<@Valid DoctorPrescriptionItemRequestDto> items;
}
