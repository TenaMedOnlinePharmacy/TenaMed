package com.TenaMed.doctor.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class CreateDoctorPrescriptionResponseDto {

    private UUID patientId;
    private UUID prescriptionId;
    private UUID doctorId;
    private UUID hospitalId;
    private LocalDate issueDate;
    private LocalDate expiryDate;
}
