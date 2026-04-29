package com.TenaMed.doctor.dto;

public record DoctorAssignedPrescriptionItemResponseDto(
    Integer quantity,
    String medicineName,
    String form,
    String instruction,
    String strength
) {
}

