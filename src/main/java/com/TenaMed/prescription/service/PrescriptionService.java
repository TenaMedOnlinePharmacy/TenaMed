package com.TenaMed.prescription.service;

import com.TenaMed.prescription.entity.Prescription;

import java.util.UUID;

public interface PrescriptionService {

    Prescription createUploadedPrescription();

    Prescription attachOcrDates(UUID id, String createdDate, String expirationDate);

    Prescription createFromOcrDates(String createdDate, String expirationDate);

    Prescription getPrescription(UUID id);

    boolean isPrescriptionValid(UUID id);
}
