package com.TenaMed.pharmacy.service.impl;

import com.TenaMed.pharmacy.service.PrescriptionInventoryMatchService;
import com.TenaMed.verification.event.PrescriptionVerifiedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PrescriptionVerifiedEventListener {

    private final PrescriptionInventoryMatchService prescriptionInventoryMatchService;

    public PrescriptionVerifiedEventListener(PrescriptionInventoryMatchService prescriptionInventoryMatchService) {
        this.prescriptionInventoryMatchService = prescriptionInventoryMatchService;
    }

    @EventListener
    public void handlePrescriptionVerified(PrescriptionVerifiedEvent event) {
        System.out.println(
                prescriptionInventoryMatchService.findInventoryMatchesByPrescription(event.getPrescriptionId())
                        .stream()
                        .map(x -> "pharmacyId=" + x.getPharmacyId() + ", medicineId=" + x.getMedicineId())
                        .toList()
        );    }
}