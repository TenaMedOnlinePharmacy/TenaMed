package com.TenaMed.prescription.service;

import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.doctor.dto.DoctorPrescriptionItemRequestDto;

import java.util.List;
import java.util.UUID;

public interface PrescriptionService {

    Prescription createUploadedPrescription(UUID userId);

    Prescription attachOcrDates(UUID id, String createdDate, String expirationDate);

    Prescription createFromOcrDates(String createdDate, String expirationDate);

    Prescription getPrescription(UUID id);

    boolean isPrescriptionValid(UUID id);

    Prescription createDoctorPrescription(UUID patientId,
                                          UUID hospitalId,
                                          UUID doctorId,
                                          java.time.LocalDate expiryDate,
                                          Integer maxRefillsAllowed,
                                          String uniqueCode,
                                          Boolean highRisk,
                                          com.TenaMed.prescription.entity.PrescriptionType type,
                                          List<DoctorPrescriptionItemRequestDto> items);

    com.TenaMed.prescription.dto.PrescriptionResponseDto getPrescriptionDetails(String uniqueCode, String phone);
}
