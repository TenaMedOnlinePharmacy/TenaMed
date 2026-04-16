package com.TenaMed.pharmacy.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
public class PharmacistInviteRegistrationRequestDto {

    @Valid
    @NotNull
    private UserDto user;

    @Valid
    @NotNull
    private PharmacistDto pharmacist;

    @Data
    public static class UserDto {
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

        private String phone;

        private Map<String, Object> address;
    }

    @Data
    public static class PharmacistDto {
        private String licenseNumber;
        private LocalDate licenseExpiry;
        private Boolean canVerifyPrescriptions;
        private Boolean canManageInventory;
    }
}
