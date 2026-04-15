package com.TenaMed.doctor.controller;

import com.TenaMed.common.security.CurrentUserProvider;
import com.TenaMed.doctor.service.DoctorOnboardingService;
import com.TenaMed.doctor.dto.DoctorResponseDto;
import com.TenaMed.doctor.entity.DoctorStatus;
import com.TenaMed.doctor.service.DoctorService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorController.class)
@AutoConfigureMockMvc(addFilters = false)
class DoctorControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DoctorService doctorService;

    @MockitoBean
        private DoctorOnboardingService doctorOnboardingService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void shouldRegisterUserAndCreateDoctorFromInvite() throws Exception {
        UUID hospitalId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        DoctorResponseDto response = new DoctorResponseDto();
        response.setId(doctorId);
        response.setUserId(UUID.randomUUID());
        response.setHospitalId(hospitalId);
        response.setLicenseNumber("DOC-1001");
        response.setStatus(DoctorStatus.PENDING);

        when(doctorOnboardingService.registerAndCreateDoctorFromInvite(eq("invite-token"), any())).thenReturn(response);

        mockMvc.perform(post("/api/doctors/create")
                        .param("token", "invite-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "user", java.util.Map.of(
                                        "email", "doctor@example.com",
                                        "password", "StrongPass123",
                                        "firstName", "Abel",
                                        "lastName", "Kassa",
                                        "phone", "+251900000001"
                                ),
                                "doctor", java.util.Map.of(
                                        "licenseNumber", "DOC-1001",
                                        "specialization", "Cardiology"
                                )
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hospitalId").value(hospitalId.toString()));

        verify(doctorOnboardingService).registerAndCreateDoctorFromInvite(eq("invite-token"), any());
    }

    @Test
    void shouldRejectWhenTokenMissing() throws Exception {
        mockMvc.perform(post("/api/doctors/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "user", java.util.Map.of(
                                        "email", "doctor@example.com",
                                        "password", "StrongPass123",
                                        "firstName", "Abel",
                                        "lastName", "Kassa"
                                ),
                                "doctor", java.util.Map.of(
                                        "licenseNumber", "DOC-1001"
                                )
                        ))))
                .andExpect(status().isBadRequest());

        verify(doctorOnboardingService, never()).registerAndCreateDoctorFromInvite(any(), any());
    }
}
