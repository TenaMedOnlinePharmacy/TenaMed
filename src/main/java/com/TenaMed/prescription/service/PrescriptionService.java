package com.TenaMed.prescription.service;

import com.TenaMed.prescription.entity.Prescription;

public interface PrescriptionService {

    Prescription createFromOcrDates(String createdDate, String expirationDate);
}
