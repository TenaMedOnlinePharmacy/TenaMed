package com.TenaMed.prescription.service.impl;

import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import com.TenaMed.prescription.service.PrescriptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Service
@Transactional
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;

    public PrescriptionServiceImpl(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    @Override
    public Prescription createFromOcrDates(String createdDate, String expirationDate) {
        Prescription prescription = new Prescription();
        prescription.setIssueDate(parseToLocalDate(createdDate));
        prescription.setExpiryDate(parseToLocalDate(expirationDate));
        return prescriptionRepository.save(prescription);
    }

    private LocalDate parseToLocalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (Exception ignored) {
        }

        try {
            return LocalDateTime.parse(value).toLocalDate();
        } catch (Exception ignored) {
        }

        try {
            return OffsetDateTime.parse(value).toLocalDate();
        } catch (Exception ignored) {
            return null;
        }
    }
}
