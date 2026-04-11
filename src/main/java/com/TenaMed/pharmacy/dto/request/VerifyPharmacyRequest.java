package com.TenaMed.pharmacy.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class VerifyPharmacyRequest {

    @NotNull
    private UUID verifiedBy;
}