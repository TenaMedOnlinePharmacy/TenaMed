package com.TenaMed.pharmacy.controller;

import com.TenaMed.pharmacy.dto.response.StaffResponse;
import com.TenaMed.pharmacy.enums.EmploymentStatus;
import com.TenaMed.pharmacy.enums.StaffRole;
import com.TenaMed.pharmacy.service.PharmacistOnboardingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PharmacistController.class)
@AutoConfigureMockMvc(addFilters = false)
class PharmacistControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PharmacistOnboardingService pharmacistOnboardingService;

    @Test
    void shouldCreatePharmacistFromInvite() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID pharmacyId = UUID.randomUUID();

        StaffResponse response = StaffResponse.builder()
                .userId(userId)
                .pharmacyId(pharmacyId)
                .staffRole(StaffRole.PHARMACIST)
                .employmentStatus(EmploymentStatus.ACTIVE)
                .build();

        when(pharmacistOnboardingService.registerAndCreatePharmacistFromInvite(eq("invite-token"), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/pharmacists/create")
                        .param("token", "invite-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "user", Map.of(
                                        "email", "pharmacist@example.com",
                                        "password", "StrongPass123",
                                        "firstName", "Sara",
                                        "lastName", "Abebe"
                                ),
                                "pharmacist", Map.of(
                                        "licenseNumber", "PH-1001"
                                )
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.staffRole").value("PHARMACIST"));

        verify(pharmacistOnboardingService).registerAndCreatePharmacistFromInvite(eq("invite-token"), any());
    }

    @Test
    void shouldRejectWhenTokenMissing() throws Exception {
        mockMvc.perform(post("/api/pharmacists/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "user", Map.of(
                                        "email", "pharmacist@example.com",
                                        "password", "StrongPass123",
                                        "firstName", "Sara",
                                        "lastName", "Abebe"
                                ),
                                "pharmacist", Map.of(
                                        "licenseNumber", "PH-1001"
                                )
                        ))))
                .andExpect(status().isBadRequest());

        verify(pharmacistOnboardingService, never()).registerAndCreatePharmacistFromInvite(any(), any());
    }
}
