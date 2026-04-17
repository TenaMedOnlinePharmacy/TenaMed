package com.TenaMed.medicine.mapper;

import com.TenaMed.medicine.dto.MedicineRequestDto;
import com.TenaMed.medicine.dto.MedicineResponseDto;
import com.TenaMed.medicine.entity.Medicine;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MedicineMapper {

    public Medicine toEntity(MedicineRequestDto dto) {
        Medicine medicine = new Medicine();
        dto(dto, medicine);
        return medicine;
    }

    public MedicineResponseDto toResponseDto(Medicine medicine) {
        return MedicineResponseDto.builder()
                .id(medicine.getId())
                .name(medicine.getName())
                .genericName(medicine.getGenericName())
                .category(medicine.getCategory() != null ? medicine.getCategory().getName() : null)
                .dosageForm(medicine.getDosageForm() != null ? medicine.getDosageForm().getName() : null)
                .therapeuticClass(medicine.getTherapeuticClass())
                .schedule(medicine.getSchedule())
                .needManualReview(medicine.isNeedManualReview())
                .doseValue(medicine.getDoseValue())
                .doseUnit(medicine.getDoseUnit())
                .regulatoryCode(medicine.getRegulatoryCode())
                .requiresPrescription(medicine.isRequiresPrescription())
                .indications(medicine.getIndications())
                .contraindications(medicine.getContraindications())
                .sideEffects(medicine.getSideEffects())
                .dosageInstructions(medicine.getDosageInstructions())
                .pregnancyCategory(medicine.getPregnancyCategory())
                .allergenIds(medicine.getMedicineAllergens().stream()
                    .map(link -> link.getAllergen().getId())
                    .toList())
                .dopingRuleIds(List.of())
                .build();
    }

    public void updateEntityFromDto(MedicineRequestDto dto, Medicine medicine) {
        dto(dto, medicine);
    }

    private void dto(MedicineRequestDto dto, Medicine medicine) {
        medicine.setName(dto.getName());
        medicine.setGenericName(dto.getGenericName());
        medicine.setTherapeuticClass(dto.getTherapeuticClass());
        medicine.setSchedule(dto.getSchedule());
        medicine.setNeedManualReview(Boolean.TRUE.equals(dto.getNeedManualReview()));
        medicine.setDoseValue(dto.getDoseValue());
        medicine.setDoseUnit(dto.getDoseUnit());
        medicine.setRegulatoryCode(dto.getRegulatoryCode());
        medicine.setRequiresPrescription(dto.isRequiresPrescription());
        medicine.setIndications(dto.getIndications());
        medicine.setContraindications(dto.getContraindications());
        medicine.setSideEffects(dto.getSideEffects());
        medicine.setDosageInstructions(dto.getDosageInstructions());
        medicine.setPregnancyCategory(dto.getPregnancyCategory());
    }
}
