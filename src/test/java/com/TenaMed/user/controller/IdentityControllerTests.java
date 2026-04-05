package com.TenaMed.user.controller;

import com.TenaMed.user.dto.LoginRequestDto;
import com.TenaMed.user.dto.LoginResponseDto;
import com.TenaMed.user.dto.RegisterRequestDto;
import com.TenaMed.user.dto.RegisterResponseDto;
import com.TenaMed.user.service.IdentityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IdentityController.class)
@AutoConfigureMockMvc(addFilters = false)
class IdentityControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IdentityService identityService;

    @Test
    void shouldRegisterUser() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("test@tenamed.com");
        request.setPassword("StrongPass123");
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setPhone("+251900000000");
        request.setAddress(Map.of("city", "Addis Ababa"));
        request.setRoleNames(Set.of("PATIENT"));

        RegisterResponseDto response = RegisterResponseDto.builder()
                .accountId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .email("test@tenamed.com")
                .accountStatus("ACTIVE")
                .roles(List.of("PATIENT"))
                .createdAt(LocalDateTime.now())
                .build();

        when(identityService.register(any(RegisterRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/identity/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@tenamed.com"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }

    @Test
    void shouldLoginUser() throws Exception {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("test@tenamed.com");
        request.setPassword("StrongPass123");

        LoginResponseDto response = LoginResponseDto.builder()
                .accountId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .email("test@tenamed.com")
                .accountStatus("ACTIVE")
                .roles(List.of("PATIENT"))
                .build();

        when(identityService.login(any(LoginRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/identity/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@tenamed.com"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }
}
