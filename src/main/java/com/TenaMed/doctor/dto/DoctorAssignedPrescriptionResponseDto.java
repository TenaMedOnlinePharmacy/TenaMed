package com.TenaMed.doctor.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DoctorAssignedPrescriptionResponseDto(
    UUID prescriptionId,
    String uniqueCode,
    String patientFullName,
    LocalDate expiryDate,
    List<DoctorAssignedPrescriptionItemResponseDto> prescriptionItems
) {
}


