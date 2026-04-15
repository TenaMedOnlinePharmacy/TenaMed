package com.TenaMed.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class RegisterPharmacistRequestDto {

    @Valid
    @NotNull
    private OwnerDto pharmacist;

    @Valid
    @NotNull
    private PharmacyDto pharmacy;

    @Getter
    @Setter
    public static class OwnerDto {
        @Email
        @NotBlank
        private String email;

        @NotBlank
        @Size(min = 8, max = 72)
        private String password;

        @NotBlank
        private String firstName;

        @NotBlank
        private String lastName;

        @NotBlank
        private String phone;
    }

    @Getter
    @Setter
    public static class PharmacyDto {
        @NotBlank
        private String name;

        @NotBlank
        private String legalName;

        @NotBlank
        private String licenseNumber;

        @Email
        @NotBlank
        private String email;

        @NotBlank
        private String phone;

        private String website;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String pharmacyType;
        private String operatingHours;
        private Boolean is24Hours;
        private Boolean hasDelivery;

        @NotNull
        private MultipartFile licenseImage;
    }
}