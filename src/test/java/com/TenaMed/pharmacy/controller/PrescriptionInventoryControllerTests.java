package com.TenaMed.pharmacy.controller;

import com.TenaMed.medicine.dto.MedicinePharmacySearchResponseDto;
import com.TenaMed.pharmacy.exception.PharmacyValidationException;
import com.TenaMed.pharmacy.service.PrescriptionInventoryMatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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

        when(prescriptionInventoryMatchService.findInventoryMatchesByPrescription(prescriptionId))
            .thenReturn(List.of(MedicinePharmacySearchResponseDto.builder()
                .medicineName("Paracetamol")
                .prescriptionRequired(true)
                .pharmacyLegalName("City Pharmacy Ltd")
                .price(new BigDecimal("99.99"))
                .medicineCategory("Analgesics")
                .imageUrl("https://img/med.png")
                .indications("Pain")
                .contraindications("Allergy")
                .sideEffects("Nausea")
                .build()));

        mockMvc.perform(get("/api/pharmacy/prescriptions/{prescriptionId}/inventory-matches", prescriptionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].medicineName").value("Paracetamol"))
            .andExpect(jsonPath("$[0].prescriptionRequired").value(true))
            .andExpect(jsonPath("$[0].pharmacyLegalName").value("City Pharmacy Ltd"))
            .andExpect(jsonPath("$[0].price").value(99.99))
            .andExpect(jsonPath("$[0].medicineCategory").value("Analgesics"));
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