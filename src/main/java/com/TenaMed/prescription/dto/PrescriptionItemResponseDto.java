package com.TenaMed.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItemResponseDto {
    private UUID prescriptionItemId;
    private UUID medicineId;
    private String name;
    private Integer quantity;
    private String from; 
    private String instruction;
    private String strength;
}
