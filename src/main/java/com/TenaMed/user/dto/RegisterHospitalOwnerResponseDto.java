package com.TenaMed.user.dto;

import com.TenaMed.hospital.dto.HospitalResponseDto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterHospitalOwnerResponseDto {
    private RegisterResponseDto owner;
    private HospitalResponseDto hospital;
}
