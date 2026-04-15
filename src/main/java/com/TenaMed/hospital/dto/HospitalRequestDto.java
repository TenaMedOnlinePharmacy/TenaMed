package com.TenaMed.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HospitalRequestDto {

    @NotBlank
    private String name;

    @NotBlank
    private String licenseNumber;

    private String licenseImageUrl;
}
