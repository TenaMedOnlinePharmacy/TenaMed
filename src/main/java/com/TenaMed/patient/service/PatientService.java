package com.TenaMed.patient.service;

import com.TenaMed.patient.dto.AddAllergyDto;
import com.TenaMed.patient.dto.PatientProfileResponse;
import com.TenaMed.patient.dto.UpdateProfileDto;

import java.util.List;
import java.util.UUID;

public interface PatientService {

    PatientProfileResponse createProfile(UUID userId);

    PatientProfileResponse getProfileByUserId(UUID userId);

    PatientProfileResponse updateProfile(UUID userId, UpdateProfileDto dto);

    PatientProfileResponse addAllergy(UUID userId, AddAllergyDto dto);

    void removeAllergy(UUID userId, UUID allergyId);

    void convertTemporaryPatient(UUID patientId, UUID userId);

    List<PatientProfileResponse.AllergyItem> getAllergies(UUID userId);
}
