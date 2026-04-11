package com.TenaMed.pharmacy.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreatePharmacyRequest {

    @NotBlank
    private String name;

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
    private UUID ownerId;
}