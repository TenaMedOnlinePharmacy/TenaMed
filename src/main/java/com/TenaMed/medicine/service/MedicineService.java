package com.TenaMed.medicine.service;

import com.TenaMed.medicine.dto.MedicineRequestDto;
import com.TenaMed.medicine.dto.MedicineResponseDto;
import com.TenaMed.medicine.dto.MedicineSearchDto;
import com.TenaMed.medicine.dto.MedicinePharmacySearchResponseDto;
import com.TenaMed.medicine.dto.MedicineDopingRuleRequestDto;
import com.TenaMed.medicine.dto.MedicineDopingRuleResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MedicineService {

    MedicineResponseDto createMedicine(MedicineRequestDto requestDto);

    MedicineResponseDto createMedicine(MedicineRequestDto requestDto, MultipartFile image);

    MedicineResponseDto getMedicineById(UUID id);

    List<MedicineResponseDto> getAllMedicines();

    List<MedicinePharmacySearchResponseDto> searchMedicines(MedicineSearchDto searchDto);

    MedicineResponseDto updateMedicine(UUID id, MedicineRequestDto requestDto);

    MedicineResponseDto addAllergenToMedicine(UUID medicineId, UUID allergenId);

    MedicineResponseDto removeAllergenFromMedicine(UUID medicineId, UUID allergenId);

    MedicineDopingRuleResponseDto addDopingRuleToMedicine(UUID medicineId, MedicineDopingRuleRequestDto requestDto);

    void removeDopingRuleFromMedicine(UUID medicineId, UUID ruleId);

    void deleteMedicine(UUID id);
}
