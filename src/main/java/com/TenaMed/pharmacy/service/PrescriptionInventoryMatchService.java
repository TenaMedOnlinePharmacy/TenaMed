package com.TenaMed.pharmacy.service;

import com.TenaMed.medicine.dto.MedicinePharmacySearchResponseDto;

import java.util.List;
import java.util.UUID;

public interface PrescriptionInventoryMatchService {

    List<MedicinePharmacySearchResponseDto> findInventoryMatchesByPrescription(UUID prescriptionId);
}