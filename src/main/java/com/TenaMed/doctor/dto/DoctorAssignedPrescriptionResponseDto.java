package com.TenaMed.doctor.dto;

import java.util.List;
import java.util.UUID;

public record DoctorAssignedPrescriptionResponseDto(
    UUID prescriptionId,
    String uniqueCode,
    String patientFullName,
    List<DoctorAssignedPrescriptionItemResponseDto> prescriptionItems
) {
}

