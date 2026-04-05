package com.TenaMed.medicine.controller;

import com.TenaMed.medicine.dto.MedicineDopingRuleRequestDto;
import com.TenaMed.medicine.dto.MedicineDopingRuleResponseDto;
import com.TenaMed.medicine.dto.MedicineResponseDto;
import com.TenaMed.medicine.service.MedicineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MedicineController.class)
@AutoConfigureMockMvc(addFilters = false)
class MedicineControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @MockitoBean
    private MedicineService medicineService;

    @Test
    void shouldAddAllergenToMedicine() throws Exception {
        UUID medicineId = UUID.randomUUID();
        UUID allergenId = UUID.randomUUID();

        MedicineResponseDto responseDto = MedicineResponseDto.builder()
                .id(medicineId)
                .name("Paracetamol")
                .build();

        when(medicineService.addAllergenToMedicine(medicineId, allergenId)).thenReturn(responseDto);

        mockMvc.perform(post("/api/medicines/{medicineId}/allergens/{allergenId}", medicineId, allergenId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(medicineId.toString()))
                .andExpect(jsonPath("$.name").value("Paracetamol"));

        verify(medicineService).addAllergenToMedicine(medicineId, allergenId);
    }

    @Test
    void shouldRemoveAllergenFromMedicine() throws Exception {
        UUID medicineId = UUID.randomUUID();
        UUID allergenId = UUID.randomUUID();

        MedicineResponseDto responseDto = MedicineResponseDto.builder()
                .id(medicineId)
                .name("Paracetamol")
                .build();

        when(medicineService.removeAllergenFromMedicine(medicineId, allergenId)).thenReturn(responseDto);

        mockMvc.perform(delete("/api/medicines/{medicineId}/allergens/{allergenId}", medicineId, allergenId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(medicineId.toString()));

        verify(medicineService).removeAllergenFromMedicine(medicineId, allergenId);
    }

    @Test
    void shouldAddDopingRuleToMedicine() throws Exception {
        UUID medicineId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();

        MedicineDopingRuleRequestDto requestDto = new MedicineDopingRuleRequestDto();
        requestDto.setRuleset("WADA");
        requestDto.setRulesetYear(2026);
        requestDto.setStatus("PROHIBITED");
        requestDto.setNotes("Banned in competition");

        MedicineDopingRuleResponseDto responseDto = MedicineDopingRuleResponseDto.builder()
                .id(ruleId)
                .medicineId(medicineId)
                .ruleset("WADA")
                .rulesetYear(2026)
                .status("PROHIBITED")
                .notes("Banned in competition")
                .build();

        when(medicineService.addDopingRuleToMedicine(eq(medicineId), any(MedicineDopingRuleRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/medicines/{medicineId}/doping-rules", medicineId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ruleId.toString()))
                .andExpect(jsonPath("$.medicineId").value(medicineId.toString()))
                .andExpect(jsonPath("$.ruleset").value("WADA"))
                .andExpect(jsonPath("$.rulesetYear").value(2026))
                .andExpect(jsonPath("$.status").value("PROHIBITED"));

        verify(medicineService).addDopingRuleToMedicine(eq(medicineId), any(MedicineDopingRuleRequestDto.class));
    }

    @Test
    void shouldRemoveDopingRuleFromMedicine() throws Exception {
        UUID medicineId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();

        doNothing().when(medicineService).removeDopingRuleFromMedicine(medicineId, ruleId);

        mockMvc.perform(delete("/api/medicines/{medicineId}/doping-rules/{ruleId}", medicineId, ruleId))
                .andExpect(status().isNoContent());

        verify(medicineService).removeDopingRuleFromMedicine(medicineId, ruleId);
    }
}
