package com.TenaMed.pharmacy.controller;

import com.TenaMed.pharmacy.dto.response.PrescriptionInventoryMatchDto;
import com.TenaMed.pharmacy.exception.PharmacyValidationException;
import com.TenaMed.pharmacy.service.PrescriptionInventoryMatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrescriptionInventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class PrescriptionInventoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PrescriptionInventoryMatchService prescriptionInventoryMatchService;

    @Test
    void shouldReturnInventoryMatches() throws Exception {
        UUID prescriptionId = UUID.randomUUID();
        UUID prescriptionItemId = UUID.randomUUID();
        UUID pharmacyId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();

        when(prescriptionInventoryMatchService.findInventoryMatchesByPrescription(prescriptionId))
            .thenReturn(List.of(PrescriptionInventoryMatchDto.builder()
                .prescriptionId(prescriptionId)
                .prescriptionItemId(prescriptionItemId)
                .pharmacyId(pharmacyId)
                .medicineId(medicineId)
                .build()));

        mockMvc.perform(get("/api/pharmacy/prescriptions/{prescriptionId}/inventory-matches", prescriptionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].prescriptionId").value(prescriptionId.toString()))
            .andExpect(jsonPath("$[0].prescriptionItemId").value(prescriptionItemId.toString()))
            .andExpect(jsonPath("$[0].pharmacyId").value(pharmacyId.toString()))
            .andExpect(jsonPath("$[0].medicineId").value(medicineId.toString()));
    }

    @Test
    void shouldReturnBadRequestWhenServiceThrowsValidationException() throws Exception {
        UUID prescriptionId = UUID.randomUUID();

        when(prescriptionInventoryMatchService.findInventoryMatchesByPrescription(prescriptionId))
            .thenThrow(new PharmacyValidationException("Prescription not found"));

        mockMvc.perform(get("/api/pharmacy/prescriptions/{prescriptionId}/inventory-matches", prescriptionId))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Prescription not found"));
    }
}