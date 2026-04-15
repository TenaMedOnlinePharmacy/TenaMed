package com.TenaMed.user.dto;

import com.TenaMed.pharmacy.dto.response.PharmacyResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterPharmacistResponseDto {
    private RegisterResponseDto pharmacist;
    private PharmacyResponse pharmacy;
}