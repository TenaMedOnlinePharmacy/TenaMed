package com.TenaMed.medicine.service;

import com.TenaMed.medicine.dto.MedicineRequestDto;
import com.TenaMed.medicine.dto.MedicineResponseDto;
import com.TenaMed.medicine.dto.MedicineDopingRuleRequestDto;
import com.TenaMed.medicine.dto.MedicineDopingRuleResponseDto;

import java.util.List;
import java.util.UUID;

public interface MedicineService {

    MedicineResponseDto createMedicine(MedicineRequestDto requestDto);

    MedicineResponseDto getMedicineById(UUID id);

    List<MedicineResponseDto> getAllMedicines();

    List<MedicineResponseDto> searchMedicines(String name, String category, String therapeuticClass, Boolean requiresPrescription);

    MedicineResponseDto updateMedicine(UUID id, MedicineRequestDto requestDto);

    MedicineResponseDto addAllergenToMedicine(UUID medicineId, UUID allergenId);

    MedicineResponseDto removeAllergenFromMedicine(UUID medicineId, UUID allergenId);

    MedicineDopingRuleResponseDto addDopingRuleToMedicine(UUID medicineId, MedicineDopingRuleRequestDto requestDto);

    void removeDopingRuleFromMedicine(UUID medicineId, UUID ruleId);

    void deleteMedicine(UUID id);
}
