package com.TenaMed.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionResponseDto {
    private UUID prescriptionId;
    private Integer maximumRefillAllowed;
    private Integer refillUsed;
    private List<PrescriptionItemResponseDto> items;
}
