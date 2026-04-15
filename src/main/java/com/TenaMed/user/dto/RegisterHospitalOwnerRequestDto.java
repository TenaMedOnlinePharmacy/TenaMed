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
public class RegisterHospitalOwnerRequestDto {

    @Valid
    @NotNull
    private OwnerDto owner;

    @Valid
    @NotNull
    private HospitalDto hospital;

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
    public static class HospitalDto {
        @NotBlank
        private String name;

        @NotBlank
        private String licenseNumber;

        @NotNull
        private MultipartFile licenseImage;
    }
}
