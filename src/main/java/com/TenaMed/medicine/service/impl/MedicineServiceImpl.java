package com.TenaMed.medicine.service.impl;

import com.TenaMed.medicine.dto.MedicineRequestDto;
import com.TenaMed.medicine.dto.MedicineResponseDto;
import com.TenaMed.medicine.entity.Medicine;
import com.TenaMed.medicine.exception.MedicineAlreadyExistsException;
import com.TenaMed.medicine.exception.MedicineNotFoundException;
import com.TenaMed.medicine.mapper.MedicineMapper;
import com.TenaMed.medicine.repository.MedicineRepository;
import com.TenaMed.medicine.service.MedicineService;
import com.TenaMed.medicine.specification.MedicineSpecification;
import com.TenaMed.medicine.validator.MedicineValidator;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MedicineServiceImpl implements MedicineService {

    private final MedicineRepository medicineRepository;
    private final MedicineMapper medicineMapper;
    private final MedicineValidator medicineValidator;

    public MedicineServiceImpl(MedicineRepository medicineRepository,
                               MedicineMapper medicineMapper,
                               MedicineValidator medicineValidator) {
        this.medicineRepository = medicineRepository;
        this.medicineMapper = medicineMapper;
        this.medicineValidator = medicineValidator;
    }

    @Override
    public MedicineResponseDto createMedicine(MedicineRequestDto requestDto) {
        medicineValidator.validate(requestDto);
        if (medicineRepository.existsByNameIgnoreCase(requestDto.getName())) {
            throw new MedicineAlreadyExistsException(requestDto.getName());
        }
        Medicine medicine = medicineMapper.toEntity(requestDto);
        Medicine saved = medicineRepository.save(medicine);
        return medicineMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicineResponseDto getMedicineById(Long id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new MedicineNotFoundException(id));
        return medicineMapper.toResponseDto(medicine);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicineResponseDto> getAllMedicines() {
        return medicineRepository.findAll()
                .stream()
                .map(medicineMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicineResponseDto> searchMedicines(String name, String category,
                                                     String manufacturer, Boolean requiresPrescription) {
        Specification<Medicine> spec = Specification
                .where(MedicineSpecification.hasName(name))
                .and(MedicineSpecification.hasCategory(category))
                .and(MedicineSpecification.hasManufacturer(manufacturer))
                .and(MedicineSpecification.requiresPrescription(requiresPrescription));

        return medicineRepository.findAll(spec)
                .stream()
                .map(medicineMapper::toResponseDto)
                .toList();
    }

    @Override
    public MedicineResponseDto updateMedicine(Long id, MedicineRequestDto requestDto) {
        medicineValidator.validate(requestDto);
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new MedicineNotFoundException(id));
        medicineMapper.updateEntityFromDto(requestDto, medicine);
        Medicine updated = medicineRepository.save(medicine);
        return medicineMapper.toResponseDto(updated);
    }

    @Override
    public void deleteMedicine(Long id) {
        if (!medicineRepository.existsById(id)) {
            throw new MedicineNotFoundException(id);
        }
        medicineRepository.deleteById(id);
    }
}
