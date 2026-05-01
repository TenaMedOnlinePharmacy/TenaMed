package com.TenaMed.user.controller;

import com.TenaMed.user.dto.AssignRoleRequestDto;
import com.TenaMed.user.dto.UserDetailsResponseDto;
import com.TenaMed.user.dto.UserRolesResponseDto;
import com.TenaMed.user.service.IdentityService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IdentityService identityService;

    @Test
    void shouldGetUserDetails() throws Exception {
        UUID userId = UUID.randomUUID();
        UserDetailsResponseDto response = UserDetailsResponseDto.builder()
                .userId(userId)
                .email("test@tenamed.com")
                .firstName("Jane")
                .lastName("Doe")
                .roles(List.of("PATIENT"))
                .build();

        when(identityService.getUserDetails(userId)).thenReturn(response);

        mockMvc.perform(get("/api/admin/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@tenamed.com"));
    }

    @Test
    void shouldAssignRoleToUser() throws Exception {
        UUID userId = UUID.randomUUID();
        AssignRoleRequestDto requestDto = new AssignRoleRequestDto();
        requestDto.setRoleName("ADMIN");

        UserRolesResponseDto response = UserRolesResponseDto.builder()
                .userId(userId)
                .roles(List.of("ADMIN", "PATIENT"))
                .build();

        when(identityService.assignRoleToUser(userId, "ADMIN")).thenReturn(response);

        mockMvc.perform(post("/api/admin/users/{id}/roles", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
    }

    @Test
    void shouldRemoveRoleFromUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UserRolesResponseDto response = UserRolesResponseDto.builder()
                .userId(userId)
                .roles(List.of("PATIENT"))
                .build();

        when(identityService.removeRoleFromUser(userId, "ADMIN")).thenReturn(response);

        mockMvc.perform(delete("/api/admin/users/{id}/roles/{roleName}", userId, "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("PATIENT"));
    }

    @Test
    void shouldGetUserRoles() throws Exception {
        UUID userId = UUID.randomUUID();
        UserRolesResponseDto response = UserRolesResponseDto.builder()
                .userId(userId)
                .roles(List.of("ADMIN", "PATIENT"))
                .build();

        when(identityService.getUserRoles(userId)).thenReturn(response);

        mockMvc.perform(get("/api/admin/users/{id}/roles", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[1]").value("PATIENT"));
    }

        @Test
        void shouldPopulateRoles() throws Exception {
                when(identityService.populateRoles()).thenReturn(List.of("ADMIN", "PATIENT", "DOCTOR", "PHARMACIST"));

                mockMvc.perform(post("/api/admin/users/roles/populate"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.createdCount").value(4))
                                .andExpect(jsonPath("$.createdRoles[0]").value("ADMIN"));
        }
}
