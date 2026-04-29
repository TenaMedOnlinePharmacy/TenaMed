package com.TenaMed.doctor.dto;

import java.util.UUID;

public record DoctorAssignedPrescriptionItemResponseDto(
    UUID medicineId,
    Integer quantity,
    String medicineName,
    String form,
    String instruction,
    String strength
) {
}


