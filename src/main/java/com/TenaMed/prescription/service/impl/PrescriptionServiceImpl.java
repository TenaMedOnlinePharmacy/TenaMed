package com.TenaMed.prescription.service.impl;

import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.entity.PrescriptionType;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import com.TenaMed.prescription.service.PrescriptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

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
        prescription.setType(PrescriptionType.UPLOADED);
        return prescriptionRepository.save(prescription);
    }

    @Override
    @Transactional(readOnly = true)
    public Prescription getPrescription(UUID id) {
        return prescriptionRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPrescriptionValid(UUID id) {
        Prescription prescription = getPrescription(id);
        if (prescription == null) {
            return false;
        }

        if (Boolean.FALSE.equals(prescription.getIsVerified())) {
            return false;
        }

        if (Boolean.TRUE.equals(prescription.getIsUsed())) {
            return false;
        }

        return prescription.getExpiryDate() == null || !prescription.getExpiryDate().isBefore(LocalDate.now());
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
