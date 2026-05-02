package com.TenaMed.patient.service;

import com.TenaMed.patient.dto.AddAllergyDto;
import com.TenaMed.patient.dto.CreatePatientDto;
import com.TenaMed.patient.dto.CreateProfileDto;
import com.TenaMed.patient.dto.PatientDto;
import com.TenaMed.patient.dto.PatientProfileResponse;
import com.TenaMed.patient.dto.UpdateAllergyDto;
import com.TenaMed.patient.dto.UpdateProfileDto;

import java.util.List;
import java.util.UUID;

public interface PatientService {

    PatientProfileResponse createProfile(UUID userId, CreateProfileDto dto);

    PatientProfileResponse getProfileByUserId(UUID userId);

    PatientProfileResponse updateProfile(UUID userId, UpdateProfileDto dto);

    PatientProfileResponse addAllergy(UUID userId, AddAllergyDto dto);

    PatientProfileResponse updateAllergy(UUID userId, UUID allergyId, UpdateAllergyDto dto);

    void removeAllergy(UUID userId, UUID allergyId);

    void convertTemporaryPatient(UUID patientId, UUID userId);

    PatientDto createTemporaryPatient(CreatePatientDto dto);

    void deleteTemporaryPatient(UUID patientId);

    List<PatientProfileResponse.AllergyItem> getAllergies(UUID userId);

    List<PatientProfileResponse> getProfilesByUserId(UUID userId);
}
