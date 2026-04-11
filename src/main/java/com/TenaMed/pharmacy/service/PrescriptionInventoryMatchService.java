package com.TenaMed.pharmacy.service;

import com.TenaMed.pharmacy.dto.response.PrescriptionInventoryMatchDto;

import java.util.List;
import java.util.UUID;

public interface PrescriptionInventoryMatchService {

    List<PrescriptionInventoryMatchDto> findInventoryMatchesByPrescription(UUID prescriptionId);
}