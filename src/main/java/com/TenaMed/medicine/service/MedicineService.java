package com.TenaMed.medicine.service;

import com.TenaMed.medicine.dto.MedicineRequestDto;
import com.TenaMed.medicine.dto.MedicineResponseDto;

import java.util.List;
import java.util.UUID;

public interface MedicineService {

    MedicineResponseDto createMedicine(MedicineRequestDto requestDto);

    MedicineResponseDto getMedicineById(UUID id);

    List<MedicineResponseDto> getAllMedicines();

    List<MedicineResponseDto> searchMedicines(String name, String category, String therapeuticClass, Boolean requiresPrescription);

    MedicineResponseDto updateMedicine(UUID id, MedicineRequestDto requestDto);

    void deleteMedicine(UUID id);
}
