package com.TenaMed.pharmacy.service;

import com.TenaMed.Normalization.entity.PrescriptionItem;
import com.TenaMed.Normalization.repository.PrescriptionItemRepository;
import com.TenaMed.inventory.entity.Inventory;
import com.TenaMed.inventory.repository.InventoryRepository;
import com.TenaMed.medicine.entity.Medicine;
import com.TenaMed.pharmacy.dto.response.PrescriptionInventoryMatchDto;
import com.TenaMed.pharmacy.exception.PharmacyValidationException;
import com.TenaMed.pharmacy.service.impl.PrescriptionInventoryMatchServiceImpl;
import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionInventoryMatchServiceImplTests {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private PrescriptionItemRepository prescriptionItemRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private PrescriptionInventoryMatchServiceImpl service;

    @Test
    void shouldThrowWhenPrescriptionNotFound() {
        UUID prescriptionId = UUID.randomUUID();
        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.empty());

        assertThrows(PharmacyValidationException.class,
            () -> service.findInventoryMatchesByPrescription(prescriptionId));
    }

    @Test
    void shouldReturnMatchesFromInventoryUsingPrescriptionItemsMedicineIds() {
        UUID prescriptionId = UUID.randomUUID();
        UUID prescriptionItemId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        UUID pharmacyId = UUID.randomUUID();

        Prescription prescription = new Prescription();
        prescription.setId(prescriptionId);

        Medicine medicine = mock(Medicine.class);
        when(medicine.getId()).thenReturn(medicineId);

        PrescriptionItem item = new PrescriptionItem();
        item.setId(prescriptionItemId);
        item.setPrescription(prescription);
        item.setMedicine(medicine);

        Inventory inventory = new Inventory();
        inventory.setPharmacyId(pharmacyId);
        inventory.setMedicineId(medicineId);

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));
        when(prescriptionItemRepository.findByPrescriptionId(prescriptionId)).thenReturn(List.of(item));
        when(inventoryRepository.findByMedicineIdIn(anyCollection())).thenReturn(List.of(inventory));

        List<PrescriptionInventoryMatchDto> result = service.findInventoryMatchesByPrescription(prescriptionId);

        assertEquals(1, result.size());
        assertEquals(prescriptionId, result.getFirst().getPrescriptionId());
        assertEquals(prescriptionItemId, result.getFirst().getPrescriptionItemId());
        assertEquals(pharmacyId, result.getFirst().getPharmacyId());
        assertEquals(medicineId, result.getFirst().getMedicineId());
    }
}