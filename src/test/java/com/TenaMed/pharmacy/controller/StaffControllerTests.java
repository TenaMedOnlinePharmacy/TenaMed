package com.TenaMed.pharmacy.controller;

import com.TenaMed.common.security.CurrentUserProvider;
import com.TenaMed.pharmacy.dto.request.AddStaffRequest;
import com.TenaMed.pharmacy.dto.response.StaffResponse;
import com.TenaMed.pharmacy.enums.EmploymentStatus;
import com.TenaMed.pharmacy.enums.StaffRole;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
import com.TenaMed.pharmacy.service.StaffService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StaffController.class)
@AutoConfigureMockMvc(addFilters = false)
class StaffControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StaffService staffService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @MockitoBean
    private PharmacyRepository pharmacyRepository;

    @Test
    void shouldAddStaff() throws Exception {
        UUID pharmacyId = UUID.randomUUID();
        AddStaffRequest request = new AddStaffRequest();
        request.setUserId(UUID.randomUUID());
        request.setStaffRole(StaffRole.PHARMACIST);
        request.setEmploymentStatus(EmploymentStatus.ACTIVE);

        StaffResponse response = StaffResponse.builder()
            .id(UUID.randomUUID())
            .pharmacyId(pharmacyId)
            .staffRole(StaffRole.PHARMACIST)
            .build();

        when(staffService.addStaff(any(AddStaffRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/pharmacies/{id}/staff", pharmacyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.staffRole").value("PHARMACIST"));
    }

    @Test
    void shouldListStaff() throws Exception {
        StaffResponse response = StaffResponse.builder().id(UUID.randomUUID()).build();
        when(currentUserProvider.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(pharmacyRepository.findByOwnerId(any())).thenReturn(java.util.Optional.of(new com.TenaMed.pharmacy.entity.Pharmacy()));
        when(staffService.listStaff(any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/pharmacies/staff"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldVerifyPharmacist() throws Exception {
        UUID pharmacyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();

        StaffResponse response = StaffResponse.builder()
            .userId(userId)
                .pharmacyId(pharmacyId)
                .verifiedBy(verifierId)
                .build();

        when(currentUserProvider.getCurrentUserId()).thenReturn(verifierId);
        when(pharmacyRepository.findByOwnerId(verifierId)).thenReturn(java.util.Optional.of(new com.TenaMed.pharmacy.entity.Pharmacy()));
        when(staffService.verifyStaff(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/pharmacies/staff/{userId}/verify", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verifiedBy").value(verifierId.toString()));
    }
}