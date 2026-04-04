package com.TenaMed.medicine.service;

import com.TenaMed.medicine.dto.MedicineRequestDto;
import com.TenaMed.medicine.dto.MedicineResponseDto;

import java.util.List;

public interface MedicineService {

    MedicineResponseDto createMedicine(MedicineRequestDto requestDto);

    MedicineResponseDto getMedicineById(Long id);

    List<MedicineResponseDto> getAllMedicines();

    List<MedicineResponseDto> searchMedicines(String name, String category, String manufacturer, Boolean requiresPrescription);

    MedicineResponseDto updateMedicine(Long id, MedicineRequestDto requestDto);

    void deleteMedicine(Long id);
}
