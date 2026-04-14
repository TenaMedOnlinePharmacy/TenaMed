package com.TenaMed.patient.mapper;

import com.TenaMed.patient.dto.PatientProfileResponse;
import com.TenaMed.patient.entity.CustomerAllergy;
import com.TenaMed.patient.entity.PatientProfile;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class PatientMapper {

    public PatientProfileResponse toProfileResponse(PatientProfile profile, List<CustomerAllergy> allergies) {
        List<PatientProfileResponse.AllergyItem> allergyItems = allergies == null
                ? Collections.emptyList()
                : allergies.stream().map(this::toAllergyItem).toList();

        return PatientProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .weight(profile.getWeight())
                .height(profile.getHeight())
                .isPregnant(profile.getIsPregnant())
                .bloodType(profile.getBloodType())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .allergies(allergyItems)
                .build();
    }

    private PatientProfileResponse.AllergyItem toAllergyItem(CustomerAllergy allergy) {
        return PatientProfileResponse.AllergyItem.builder()
                .id(allergy.getId())
                .allergenId(allergy.getAllergen().getId())
                .allergenName(allergy.getAllergen().getName())
                .allergenCode(allergy.getAllergen().getCode())
                .allergenType(allergy.getAllergen().getAllergenType())
                .severity(allergy.getSeverity())
                .createdAt(allergy.getCreatedAt())
                .build();
    }
}
