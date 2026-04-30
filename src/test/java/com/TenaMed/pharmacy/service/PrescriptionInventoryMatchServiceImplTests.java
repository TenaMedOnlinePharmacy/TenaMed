package com.TenaMed.pharmacy.service;

import com.TenaMed.Normalization.entity.PrescriptionItem;
import com.TenaMed.Normalization.repository.PrescriptionItemRepository;
import com.TenaMed.inventory.entity.Batch;
import com.TenaMed.inventory.entity.Inventory;
import com.TenaMed.inventory.enums.BatchStatus;
import com.TenaMed.inventory.repository.BatchRepository;
import com.TenaMed.inventory.repository.InventoryRepository;
import com.TenaMed.medicine.dto.MedicinePharmacySearchResponseDto;
import com.TenaMed.medicine.entity.Category;
import com.TenaMed.medicine.entity.Medicine;
import com.TenaMed.medicine.entity.Product;
import com.TenaMed.medicine.repository.MedicineRepository;
import com.TenaMed.medicine.repository.ProductRepository;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.exception.PharmacyValidationException;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
import com.TenaMed.pharmacy.service.impl.PrescriptionInventoryMatchServiceImpl;
import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
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

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private PharmacyRepository pharmacyRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private ProductRepository productRepository;

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
        UUID inventoryId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        UUID pharmacyId = UUID.randomUUID();

        Prescription prescription = new Prescription();
        prescription.setId(prescriptionId);

        Category category = new Category();
        category.setName("Analgesics");

        Medicine medicine = new Medicine();
        ReflectionTestUtils.setField(medicine, "id", medicineId);
        medicine.setName("Paracetamol");
        medicine.setRequiresPrescription(false);
        ReflectionTestUtils.setField(medicine, "requiresPrescription", false);
        medicine.setCategory(category);
        medicine.setImageUrl("https://img/med.png");
        medicine.setIndications("Pain");
        medicine.setContraindications("Allergy");
        medicine.setSideEffects("Nausea");

        PrescriptionItem item = new PrescriptionItem();
        item.setPrescription(prescription);
        item.setMedicine(medicine);

        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setId(pharmacyId);
        pharmacy.setLegalName("City Pharmacy Ltd");

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setMedicine(medicine);
        product.setBrandName("Paracetamol Brand");

        Inventory inventory = new Inventory();
        inventory.setId(inventoryId);
        inventory.setPharmacyId(pharmacyId);
        inventory.setProductId(product.getId());

        Batch batch = new Batch();
        batch.setInventory(inventory);
        batch.setQuantity(10);
        batch.setSellingPrice(new BigDecimal("99.99"));
        batch.setStatus(BatchStatus.ACTIVE);

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));
        when(prescriptionItemRepository.findByPrescriptionId(prescriptionId)).thenReturn(List.of(item));
        when(inventoryRepository.findByProductIdIn(anyCollection())).thenReturn(List.of(inventory));
        when(batchRepository.findByInventoryIdIn(anyCollection())).thenReturn(List.of(batch));
        when(medicineRepository.findAllById(anyCollection())).thenReturn(List.of(medicine));
        when(pharmacyRepository.findAllById(anyCollection())).thenReturn(List.of(pharmacy));

        List<MedicinePharmacySearchResponseDto> result = service.findInventoryMatchesByPrescription(prescriptionId);

        assertEquals(1, result.size());
        assertEquals("Paracetamol", result.getFirst().getMedicineName());
        assertEquals(medicine.isRequiresPrescription(), result.getFirst().isPrescriptionRequired());
        assertEquals("City Pharmacy Ltd", result.getFirst().getPharmacyLegalName());
        assertEquals(new BigDecimal("99.99"), result.getFirst().getPrice());
        assertEquals("Analgesics", result.getFirst().getMedicineCategory());
    }
}