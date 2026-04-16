package com.TenaMed.pharmacy.controller;

import com.TenaMed.pharmacy.dto.request.CreatePharmacyRequest;
import com.TenaMed.pharmacy.dto.response.PharmacyResponse;
import com.TenaMed.pharmacy.enums.PharmacyStatus;
import com.TenaMed.invitation.dto.InvitationResponseDto;
import com.TenaMed.pharmacy.service.PharmacyService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PharmacyController.class)
@AutoConfigureMockMvc(addFilters = false)
class PharmacyControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PharmacyService pharmacyService;

    @Test
    void shouldCreatePharmacy() throws Exception {
        CreatePharmacyRequest request = new CreatePharmacyRequest();
        request.setName("Test Pharmacy");
        request.setLicenseNumber("LIC-100");
        request.setEmail("test@pharmacy.com");
        request.setPhone("+251900000001");
        request.setOwnerId(UUID.randomUUID());

        PharmacyResponse response = PharmacyResponse.builder()
            .id(UUID.randomUUID())
            .name("Test Pharmacy")
            .status(PharmacyStatus.PENDING)
            .build();

        when(pharmacyService.createPharmacy(any(CreatePharmacyRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/pharmacies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test Pharmacy"));
    }

    @Test
    void shouldGetPharmacy() throws Exception {
        UUID id = UUID.randomUUID();
        PharmacyResponse response = PharmacyResponse.builder().id(id).name("P1").build();
        when(pharmacyService.getPharmacy(id)).thenReturn(response);

        mockMvc.perform(get("/api/pharmacies/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void shouldVerifyPharmacy() throws Exception {
        UUID id = UUID.randomUUID();
        PharmacyResponse response = PharmacyResponse.builder().id(id).status(PharmacyStatus.VERIFIED).build();

        when(pharmacyService.verifyPharmacy(id)).thenReturn(response);

        mockMvc.perform(post("/api/pharmacies/{id}/verify", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("VERIFIED"));
    }

    @Test
    void shouldInvitePharmacist() throws Exception {
        UUID id = UUID.randomUUID();
        InvitationResponseDto response = new InvitationResponseDto();
        response.setId(UUID.randomUUID());
        response.setRole(com.TenaMed.invitation.entity.InvitationRole.PHARMACIST);
        response.setPharmacyId(id);
        response.setEmail("pharmacist@example.com");

        when(pharmacyService.invitePharmacist(id, "pharmacist@example.com")).thenReturn(response);

        mockMvc.perform(post("/api/pharmacies/{id}/invite-pharmacist", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("email", "pharmacist@example.com"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("PHARMACIST"));
    }
}