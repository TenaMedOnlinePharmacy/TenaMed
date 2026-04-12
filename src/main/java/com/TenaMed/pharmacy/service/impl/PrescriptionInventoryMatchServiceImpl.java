package com.TenaMed.pharmacy.service.impl;

import com.TenaMed.Normalization.entity.PrescriptionItem;
import com.TenaMed.Normalization.repository.PrescriptionItemRepository;
import com.TenaMed.inventory.entity.Inventory;
import com.TenaMed.inventory.repository.InventoryRepository;
import com.TenaMed.pharmacy.dto.response.PrescriptionInventoryMatchDto;
import com.TenaMed.pharmacy.exception.PharmacyValidationException;
import com.TenaMed.pharmacy.service.PrescriptionInventoryMatchService;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PrescriptionInventoryMatchServiceImpl implements PrescriptionInventoryMatchService {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final InventoryRepository inventoryRepository;

    public PrescriptionInventoryMatchServiceImpl(PrescriptionRepository prescriptionRepository,
                                                 PrescriptionItemRepository prescriptionItemRepository,
                                                 InventoryRepository inventoryRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public List<PrescriptionInventoryMatchDto> findInventoryMatchesByPrescription(UUID prescriptionId) {
        boolean exists = prescriptionRepository.findById(prescriptionId).isPresent();
        if (!exists) {
            throw new PharmacyValidationException("Prescription not found: " + prescriptionId);
        }

        List<PrescriptionItem> prescriptionItems = prescriptionItemRepository.findByPrescriptionId(prescriptionId);
        if (prescriptionItems.isEmpty()) {
            return List.of();
        }

        Set<UUID> medicineIds = prescriptionItems.stream()
            .map(item -> item.getMedicine().getId())
            .collect(Collectors.toSet());

        List<Inventory> inventories = inventoryRepository.findByMedicineIdIn(medicineIds);

        Map<UUID, List<Inventory>> inventoriesByMedicineId = inventories.stream()
            .collect(Collectors.groupingBy(Inventory::getMedicineId));

        return prescriptionItems.stream()
            .flatMap(item -> {
                UUID medicineId = item.getMedicine().getId();
                List<Inventory> matches = inventoriesByMedicineId.getOrDefault(medicineId, List.of());
                return matches.stream().map(inv -> PrescriptionInventoryMatchDto.builder()
                    .prescriptionId(prescriptionId)
                    .prescriptionItemId(item.getId())
                    .pharmacyId(inv.getPharmacyId())
                    .medicineId(inv.getMedicineId())
                    .build());
            })
            .toList();
    }
}