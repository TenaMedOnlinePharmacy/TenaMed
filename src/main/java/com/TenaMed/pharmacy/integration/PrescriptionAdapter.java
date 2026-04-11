package com.TenaMed.pharmacy.integration;

import com.TenaMed.pharmacy.exception.ExternalModuleException;
import com.TenaMed.pharmacy.exception.PrescriptionValidationException;
import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.service.PrescriptionService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PrescriptionAdapter {

    private final PrescriptionService prescriptionService;

    public PrescriptionAdapter(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    public Prescription getPrescription(UUID id) {
        try {
            Prescription prescription = prescriptionService.getPrescription(id);
            if (prescription == null) {
                throw new PrescriptionValidationException(id);
            }
            return prescription;
        } catch (PrescriptionValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ExternalModuleException("Failed to retrieve prescription from prescription module", ex);
        }
    }
}