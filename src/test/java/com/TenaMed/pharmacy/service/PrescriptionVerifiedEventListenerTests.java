package com.TenaMed.pharmacy.service;

import com.TenaMed.pharmacy.service.impl.PrescriptionVerifiedEventListener;
import com.TenaMed.verification.event.PrescriptionVerifiedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionVerifiedEventListenerTests {

    @Mock
    private PrescriptionInventoryMatchService prescriptionInventoryMatchService;

    @InjectMocks
    private PrescriptionVerifiedEventListener listener;

    @Test
    void shouldConsumeVerifiedEventAndInvokePharmacyServiceMethod() {
        UUID prescriptionId = UUID.randomUUID();
        PrescriptionVerifiedEvent event = new PrescriptionVerifiedEvent(prescriptionId);

        when(prescriptionInventoryMatchService.findInventoryMatchesByPrescription(prescriptionId))
            .thenReturn(List.of());

        listener.handlePrescriptionVerified(event);

        verify(prescriptionInventoryMatchService).findInventoryMatchesByPrescription(prescriptionId);
    }
}