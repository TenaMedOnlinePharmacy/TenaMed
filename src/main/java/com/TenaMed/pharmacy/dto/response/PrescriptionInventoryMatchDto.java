package com.TenaMed.pharmacy.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class PrescriptionInventoryMatchDto {

    private UUID prescriptionId;
    private UUID prescriptionItemId;
    private UUID pharmacyId;
    private UUID medicineId;
}