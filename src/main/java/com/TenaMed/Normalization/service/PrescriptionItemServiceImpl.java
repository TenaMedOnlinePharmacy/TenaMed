package com.TenaMed.Normalization.service;

import com.TenaMed.Normalization.entity.PrescriptionItem;
import com.TenaMed.Normalization.repository.PrescriptionItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PrescriptionItemServiceImpl implements PrescriptionItemService {

    private final PrescriptionItemRepository prescriptionItemRepository;

    public PrescriptionItemServiceImpl(PrescriptionItemRepository prescriptionItemRepository) {
        this.prescriptionItemRepository = prescriptionItemRepository;
    }

    @Override
    public PrescriptionItem save(PrescriptionItem prescriptionItem) {
        validateForSave(prescriptionItem);
        return prescriptionItemRepository.save(prescriptionItem);
    }

    private void validateForSave(PrescriptionItem prescriptionItem) {
        if (prescriptionItem == null) {
            throw new IllegalArgumentException("Prescription item is required");
        }
        if (prescriptionItem.getPrescription() == null) {
            throw new IllegalArgumentException("Prescription is required");
        }
        if (prescriptionItem.getMedicine() == null) {
            throw new IllegalArgumentException("Medicine is required");
        }
        if (prescriptionItem.getQuantity() != null && prescriptionItem.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }
}
