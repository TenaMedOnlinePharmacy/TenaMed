package com.TenaMed.pharmacy.integration;

import com.TenaMed.pharmacy.exception.PrescriptionValidationException;
import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.service.PrescriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionAdapterTests {

    @Mock
    private PrescriptionService prescriptionService;

    @InjectMocks
    private PrescriptionAdapter prescriptionAdapter;

    @Test
    void shouldDelegateGetPrescription() {
        UUID id = UUID.randomUUID();
        Prescription prescription = new Prescription();
        prescription.setId(id);
        when(prescriptionService.getPrescription(id)).thenReturn(prescription);

        Prescription actual = prescriptionAdapter.getPrescription(id);

        assertEquals(id, actual.getId());
    }

    @Test
    void shouldThrowWhenPrescriptionNotFound() {
        UUID id = UUID.randomUUID();
        when(prescriptionService.getPrescription(id)).thenReturn(null);

        assertThrows(PrescriptionValidationException.class, () -> prescriptionAdapter.getPrescription(id));
    }
}