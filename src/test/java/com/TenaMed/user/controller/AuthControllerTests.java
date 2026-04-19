package com.TenaMed.user.controller;

import com.TenaMed.hospital.dto.HospitalResponseDto;
import com.TenaMed.pharmacy.dto.response.PharmacyResponse;
import com.TenaMed.user.dto.RegisterAthleteRequestDto;
import com.TenaMed.user.dto.RegisterAthleteResponseDto;
import com.TenaMed.user.dto.RegisterHospitalOwnerResponseDto;
import com.TenaMed.user.dto.RegisterPharmacistResponseDto;
import com.TenaMed.user.dto.RegisterRequestDto;
import com.TenaMed.user.dto.RegisterResponseDto;
import com.TenaMed.user.service.AthleteOnboardingService;
import com.TenaMed.user.service.AuthService;
import com.TenaMed.user.service.HospitalOwnerOnboardingService;
import com.TenaMed.user.service.IdentityService;
import com.TenaMed.user.service.PharmacistOnboardingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IdentityService identityService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private HospitalOwnerOnboardingService hospitalOwnerOnboardingService;

    @MockitoBean
    private PharmacistOnboardingService pharmacistOnboardingService;

    @MockitoBean
    private AthleteOnboardingService athleteOnboardingService;

    @Test
    void shouldRegisterUserWithAuthEndpoint() throws Exception {
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

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@tenamed.com"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }

            @Test
            void shouldRegisterHospitalOwnerAndCreateHospital() throws Exception {
            UUID ownerUserId = UUID.randomUUID();
            UUID hospitalId = UUID.randomUUID();

            RegisterResponseDto ownerResponse = RegisterResponseDto.builder()
                .userId(ownerUserId)
                .email("owner@tenamed.com")
                .accountStatus("ACTIVE")
                .roles(List.of("HOSPITAL_OWNER"))
                .build();

            HospitalResponseDto hospitalResponse = new HospitalResponseDto();
            hospitalResponse.setId(hospitalId);
            hospitalResponse.setName("Central Hospital");
            hospitalResponse.setLicenseNumber("HOS-1001");
            hospitalResponse.setOwnerId(ownerUserId);

            RegisterHospitalOwnerResponseDto response = new RegisterHospitalOwnerResponseDto(ownerResponse, hospitalResponse);

            when(hospitalOwnerOnboardingService.registerHospitalOwner(any())).thenReturn(response);

            MockMultipartFile licenseImage = new MockMultipartFile(
                "hospital.licenseImage",
                "license.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake-image".getBytes()
            );

            mockMvc.perform(multipart("/api/auth/register-hospital-owner")
                    .file(licenseImage)
                    .param("owner.email", "owner@tenamed.com")
                    .param("owner.password", "StrongPass123")
                    .param("owner.firstName", "Abel")
                    .param("owner.lastName", "Kassa")
                    .param("owner.phone", "+251900000000")
                    .param("hospital.name", "Central Hospital")
                    .param("hospital.licenseNumber", "HOS-1001")
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.owner.email").value("owner@tenamed.com"))
                .andExpect(jsonPath("$.hospital.name").value("Central Hospital"));
            }

                @Test
                void shouldRegisterPharmacistAndCreatePharmacy() throws Exception {
                UUID pharmacistUserId = UUID.randomUUID();
                UUID pharmacyId = UUID.randomUUID();

                RegisterResponseDto pharmacistResponse = RegisterResponseDto.builder()
                    .userId(pharmacistUserId)
                    .email("pharmacist@tenamed.com")
                    .accountStatus("ACTIVE")
                    .roles(List.of("PHARMACYOWNER"))
                    .build();

                PharmacyResponse pharmacyResponse = PharmacyResponse.builder()
                    .id(pharmacyId)
                    .name("City Pharmacy")
                    .licenseNumber("PHR-2026-001")
                    .ownerId(pharmacistUserId)
                    .build();

                RegisterPharmacistResponseDto response = new RegisterPharmacistResponseDto(pharmacistResponse, pharmacyResponse);

                when(pharmacistOnboardingService.registerPharmacist(any())).thenReturn(response);

                MockMultipartFile licenseImage = new MockMultipartFile(
                    "pharmacy.licenseImage",
                    "license.png",
                    MediaType.IMAGE_PNG_VALUE,
                    "fake-image".getBytes()
                );

                mockMvc.perform(multipart("/api/auth/register-pharmacist")
                        .file(licenseImage)
                        .param("pharmacist.email", "pharmacist@tenamed.com")
                        .param("pharmacist.password", "StrongPass123")
                        .param("pharmacist.firstName", "Sara")
                        .param("pharmacist.lastName", "Abebe")
                        .param("pharmacist.phone", "+251911111111")
                        .param("pharmacy.name", "City Pharmacy")
                        .param("pharmacy.legalName", "City Pharmacy PLC")
                        .param("pharmacy.licenseNumber", "PHR-2026-001")
                        .param("pharmacy.email", "city@pharmacy.com")
                        .param("pharmacy.phone", "+251922222222")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.pharmacist.email").value("pharmacist@tenamed.com"))
                    .andExpect(jsonPath("$.pharmacy.name").value("City Pharmacy"));
                }

                @Test
                void shouldRegisterAthleteAndCreateAthleteProfile() throws Exception {
                UUID athleteUserId = UUID.randomUUID();

                RegisterResponseDto athleteResponse = RegisterResponseDto.builder()
                    .userId(athleteUserId)
                    .email("athlete@tenamed.com")
                    .accountStatus("ACTIVE")
                    .roles(List.of("PATIENT"))
                    .build();

                RegisterAthleteResponseDto response = new RegisterAthleteResponseDto(
                    athleteResponse,
                    new RegisterAthleteResponseDto.AthleteProfileDto(athleteUserId, true, LocalDateTime.now())
                );

                when(athleteOnboardingService.registerAthlete(any(RegisterAthleteRequestDto.class))).thenReturn(response);

                RegisterAthleteRequestDto request = new RegisterAthleteRequestDto();
                request.setEmail("athlete@tenamed.com");
                request.setPassword("StrongPass123");
                request.setFirstName("Liya");
                request.setLastName("Bekele");
                request.setPhone("+251933333333");
                request.setAddress(Map.of("city", "Addis Ababa"));
                request.setAdvisorEnabled(true);

                mockMvc.perform(post("/api/auth/register-athlete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.athlete.email").value("athlete@tenamed.com"))
                    .andExpect(jsonPath("$.athleteProfile.userId").value(athleteUserId.toString()))
                    .andExpect(jsonPath("$.athleteProfile.advisorEnabled").value(true));
                }
}
