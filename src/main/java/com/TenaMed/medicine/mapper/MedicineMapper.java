package com.TenaMed.medicine.mapper;

import com.TenaMed.medicine.dto.MedicineRequestDto;
import com.TenaMed.medicine.dto.MedicineResponseDto;
import com.TenaMed.medicine.entity.Medicine;
import com.TenaMed.medicine.entity.Product;
import com.TenaMed.medicine.entity.ProductImage;
import com.TenaMed.medicine.repository.ProductImageRepository;
import com.TenaMed.medicine.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class MedicineMapper {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;


    public Medicine toEntity(MedicineRequestDto dto) {
        Medicine medicine = new Medicine();
        dto(dto, medicine);
        return medicine;
    }

    public MedicineResponseDto toResponseDto(Medicine medicine) {
        return toResponseDto(medicine, null);
    }

    public MedicineResponseDto toResponseDto(Medicine medicine, UUID pharmacyId) {
        String imageUrl = productRepository.findFirstByMedicineId(medicine.getId())
                .flatMap(product -> productImageRepository.findByProductIdAndPharmacyIdAndIsPrimaryTrue(product.getId(), pharmacyId)
                        .map(ProductImage::getImageUrl)
                        .or(() -> productImageRepository.findFirstByProductIdAndIsPrimaryFalse(product.getId())
                                .map(ProductImage::getImageUrl)))
                .orElse(null);

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
                .imageUrl(imageUrl)
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
