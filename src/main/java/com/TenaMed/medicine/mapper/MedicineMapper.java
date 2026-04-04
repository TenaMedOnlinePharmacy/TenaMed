package com.TenaMed.medicine.mapper;

import com.TenaMed.medicine.dto.MedicineRequestDto;
import com.TenaMed.medicine.dto.MedicineResponseDto;
import com.TenaMed.medicine.entity.Medicine;
import org.springframework.stereotype.Component;

@Component
public class MedicineMapper {

    public Medicine toEntity(MedicineRequestDto dto) {
        return Medicine.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .category(dto.getCategory())
                .stockQuantity(dto.getStockQuantity())
                .manufacturer(dto.getManufacturer())
                .requiresPrescription(dto.isRequiresPrescription())
                .build();
    }

    public MedicineResponseDto toResponseDto(Medicine medicine) {
        return MedicineResponseDto.builder()
                .id(medicine.getId())
                .name(medicine.getName())
                .description(medicine.getDescription())
                .price(medicine.getPrice())
                .category(medicine.getCategory())
                .stockQuantity(medicine.getStockQuantity())
                .manufacturer(medicine.getManufacturer())
                .requiresPrescription(medicine.isRequiresPrescription())
                .inStock(medicine.getStockQuantity() != null && medicine.getStockQuantity() > 0)
                .build();
    }

    public void updateEntityFromDto(MedicineRequestDto dto, Medicine medicine) {
        medicine.setName(dto.getName());
        medicine.setDescription(dto.getDescription());
        medicine.setPrice(dto.getPrice());
        medicine.setCategory(dto.getCategory());
        medicine.setStockQuantity(dto.getStockQuantity());
        medicine.setManufacturer(dto.getManufacturer());
        medicine.setRequiresPrescription(dto.isRequiresPrescription());
    }
}
