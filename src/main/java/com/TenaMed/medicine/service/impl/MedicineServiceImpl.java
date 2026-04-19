package com.TenaMed.medicine.service.impl;

import com.TenaMed.antidoping.entity.MedicineDopingRule;
import com.TenaMed.antidoping.entity.MedicineDopingRuleStatus;
import com.TenaMed.antidoping.repository.MedicineDopingRuleRepository;
import com.TenaMed.medicine.dto.MedicineRequestDto;
import com.TenaMed.medicine.dto.MedicineResponseDto;
import com.TenaMed.medicine.dto.MedicineSearchDto;
import com.TenaMed.medicine.dto.MedicineDopingRuleRequestDto;
import com.TenaMed.medicine.dto.MedicineDopingRuleResponseDto;
import com.TenaMed.medicine.entity.Allergen;
import com.TenaMed.medicine.entity.Category;
import com.TenaMed.medicine.entity.DosageForm;
import com.TenaMed.medicine.entity.Medicine;
import com.TenaMed.medicine.entity.MedicineAllergen;
import com.TenaMed.medicine.exception.MedicineAlreadyExistsException;
import com.TenaMed.medicine.exception.MedicineNotFoundException;
import com.TenaMed.medicine.exception.MedicineValidationException;
import com.TenaMed.medicine.mapper.MedicineMapper;
import com.TenaMed.medicine.repository.AllergenRepository;
import com.TenaMed.medicine.repository.CategoryRepository;
import com.TenaMed.medicine.repository.DosageFormRepository;
import com.TenaMed.medicine.repository.MedicineAllergenRepository;
import com.TenaMed.medicine.repository.MedicineRepository;
import com.TenaMed.medicine.service.MedicineService;
import com.TenaMed.medicine.specification.MedicineSpecification;
import com.TenaMed.medicine.validator.MedicineValidator;
import com.TenaMed.events.DomainEventService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class MedicineServiceImpl implements MedicineService {

    private final MedicineRepository medicineRepository;
    private final CategoryRepository categoryRepository;
    private final DosageFormRepository dosageFormRepository;
    private final AllergenRepository allergenRepository;
    private final MedicineAllergenRepository medicineAllergenRepository;
    private final MedicineDopingRuleRepository medicineDopingRuleRepository;
    private final MedicineMapper medicineMapper;
    private final MedicineValidator medicineValidator;
    private final DomainEventService domainEventService;

    public MedicineServiceImpl(MedicineRepository medicineRepository,
                               CategoryRepository categoryRepository,
                               DosageFormRepository dosageFormRepository,
                               AllergenRepository allergenRepository,
                               MedicineAllergenRepository medicineAllergenRepository,
                               MedicineDopingRuleRepository medicineDopingRuleRepository,
                               MedicineMapper medicineMapper,
                               MedicineValidator medicineValidator,
                               DomainEventService domainEventService) {
        this.medicineRepository = medicineRepository;
        this.categoryRepository = categoryRepository;
        this.dosageFormRepository = dosageFormRepository;
        this.allergenRepository = allergenRepository;
        this.medicineAllergenRepository = medicineAllergenRepository;
        this.medicineDopingRuleRepository = medicineDopingRuleRepository;
        this.medicineMapper = medicineMapper;
        this.medicineValidator = medicineValidator;
        this.domainEventService = domainEventService;
    }

    @Override
    public MedicineResponseDto createMedicine(MedicineRequestDto requestDto) {
        medicineValidator.validate(requestDto);
        if (medicineRepository.existsByNameIgnoreCase(requestDto.getName())) {
            throw new MedicineAlreadyExistsException(requestDto.getName());
        }
        Medicine medicine = medicineMapper.toEntity(requestDto);
        medicine.setCategory(resolveOrCreateCategory(requestDto.getCategory()));
        medicine.setDosageForm(resolveOrCreateDosageForm(requestDto.getDosageForm()));
        Medicine saved = medicineRepository.save(medicine);
        domainEventService.publish(
            "MEDICINE_CREATED",
            "MEDICINE",
            saved.getId(),
            "PLATFORM",
            null,
            Map.of("name", saved.getName())
        );
        return toResponseDtoWithDopingRuleIds(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicineResponseDto getMedicineById(UUID id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new MedicineNotFoundException(id));
        return toResponseDtoWithDopingRuleIds(medicine);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicineResponseDto> getAllMedicines() {
        return medicineRepository.findAll()
                .stream()
                .map(this::toResponseDtoWithDopingRuleIds)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicineResponseDto> searchMedicines(MedicineSearchDto searchDto) {
        Specification<Medicine> spec = Specification.allOf(MedicineSpecification.hasKeyword(searchDto.getKeyword()))
                .and(MedicineSpecification.hasCategoryName(searchDto.getCategoryName()))
                .and(MedicineSpecification.hasDosageFormName(searchDto.getDosageFormName()))
                .and(MedicineSpecification.hasTherapeuticClass(searchDto.getTherapeuticClass()))
                .and(MedicineSpecification.requiresPrescription(searchDto.getRequiresPrescription()));

        return medicineRepository.findAll(spec)
                .stream()
            .map(this::toResponseDtoWithDopingRuleIds)
                .toList();
    }

    @Override
    public MedicineResponseDto updateMedicine(UUID id, MedicineRequestDto requestDto) {
        medicineValidator.validate(requestDto);
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new MedicineNotFoundException(id));
        medicineMapper.updateEntityFromDto(requestDto, medicine);
        medicine.setCategory(resolveOrCreateCategory(requestDto.getCategory()));
        medicine.setDosageForm(resolveOrCreateDosageForm(requestDto.getDosageForm()));
        Medicine updated = medicineRepository.save(medicine);
        domainEventService.publish(
            "MEDICINE_UPDATED",
            "MEDICINE",
            updated.getId(),
            "PLATFORM",
            null,
            Map.of("name", updated.getName())
        );
        return toResponseDtoWithDopingRuleIds(updated);
    }

        @Override
        public MedicineResponseDto addAllergenToMedicine(UUID medicineId, UUID allergenId) {
        Medicine medicine = medicineRepository.findById(medicineId)
            .orElseThrow(() -> new MedicineNotFoundException(medicineId));
        Allergen allergen = allergenRepository.findById(allergenId)
            .orElseThrow(() -> new MedicineNotFoundException("Allergen not found with id: " + allergenId));

        if (medicineAllergenRepository.existsByMedicine_IdAndAllergen_Id(medicineId, allergenId)) {
            throw new MedicineValidationException(List.of("Allergen is already linked to this medicine"));
        }

        MedicineAllergen link = new MedicineAllergen();
        link.setMedicine(medicine);
        link.setAllergen(allergen);
        medicineAllergenRepository.save(link);

        domainEventService.publish(
            "MEDICINE_ALLERGEN_LINKED",
            "MEDICINE",
            medicineId,
            "PLATFORM",
            null,
            Map.of("allergenId", allergenId.toString())
        );

        return toResponseDtoWithDopingRuleIds(medicineRepository.findById(medicineId)
            .orElseThrow(() -> new MedicineNotFoundException(medicineId)));
        }

        @Override
        public MedicineResponseDto removeAllergenFromMedicine(UUID medicineId, UUID allergenId) {
        medicineRepository.findById(medicineId)
            .orElseThrow(() -> new MedicineNotFoundException(medicineId));

        MedicineAllergen link = medicineAllergenRepository.findByMedicine_IdAndAllergen_Id(medicineId, allergenId)
            .orElseThrow(() -> new MedicineNotFoundException(
                "Allergen link not found for medicine id " + medicineId + " and allergen id " + allergenId));

        medicineAllergenRepository.delete(link);

        domainEventService.publish(
            "MEDICINE_ALLERGEN_UNLINKED",
            "MEDICINE",
            medicineId,
            "PLATFORM",
            null,
            Map.of("allergenId", allergenId.toString())
        );

        return toResponseDtoWithDopingRuleIds(medicineRepository.findById(medicineId)
            .orElseThrow(() -> new MedicineNotFoundException(medicineId)));
        }

        @Override
        public MedicineDopingRuleResponseDto addDopingRuleToMedicine(UUID medicineId, MedicineDopingRuleRequestDto requestDto) {
        Medicine medicine = medicineRepository.findById(medicineId)
            .orElseThrow(() -> new MedicineNotFoundException(medicineId));

        MedicineDopingRule rule = new MedicineDopingRule();
        rule.setMedicineId(medicine.getId());
        rule.setRuleset(requestDto.getRuleset().trim());
        rule.setRulesetYear(requestDto.getRulesetYear());
        rule.setStatus(parseDopingRuleStatus(requestDto.getStatus()));
        rule.setNotes(requestDto.getNotes());

        MedicineDopingRule saved = medicineDopingRuleRepository.save(rule);
        domainEventService.publish(
            "MEDICINE_DOPING_RULE_ADDED",
            "MEDICINE",
            medicineId,
            "PLATFORM",
            null,
            Map.of("ruleId", saved.getId().toString(), "status", saved.getStatus().name())
        );
        return MedicineDopingRuleResponseDto.builder()
            .id(saved.getId())
            .medicineId(medicineId)
            .ruleset(saved.getRuleset())
            .rulesetYear(saved.getRulesetYear())
            .status(saved.getStatus().name())
            .notes(saved.getNotes())
            .build();
        }

        @Override
        public void removeDopingRuleFromMedicine(UUID medicineId, UUID ruleId) {
        medicineRepository.findById(medicineId)
            .orElseThrow(() -> new MedicineNotFoundException(medicineId));

        MedicineDopingRule rule = medicineDopingRuleRepository.findByIdAndMedicineId(ruleId, medicineId)
            .orElseThrow(() -> new MedicineNotFoundException(
                "Doping rule not found with id " + ruleId + " for medicine id " + medicineId));

        medicineDopingRuleRepository.delete(rule);
        domainEventService.publish(
            "MEDICINE_DOPING_RULE_REMOVED",
            "MEDICINE",
            medicineId,
            "PLATFORM",
            null,
            Map.of("ruleId", ruleId.toString())
        );
        }

    @Override
    public void deleteMedicine(UUID id) {
        if (!medicineRepository.existsById(id)) {
            throw new MedicineNotFoundException(id);
        }
        medicineRepository.deleteById(id);
        domainEventService.publish(
                "MEDICINE_DELETED",
                "MEDICINE",
                id,
                "PLATFORM",
                null,
                Map.of()
        );
    }

    private Category resolveOrCreateCategory(String categoryName) {
        String normalized = categoryName.trim();
        return categoryRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setName(normalized);
                    return categoryRepository.save(category);
                });
    }

    private DosageForm resolveOrCreateDosageForm(String dosageFormName) {
        String normalized = dosageFormName.trim();
        return dosageFormRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> {
                    DosageForm dosageForm = new DosageForm();
                    dosageForm.setName(normalized);
                    return dosageFormRepository.save(dosageForm);
                });
    }

    private MedicineResponseDto toResponseDtoWithDopingRuleIds(Medicine medicine) {
        MedicineResponseDto response = medicineMapper.toResponseDto(medicine);
        response.setDopingRuleIds(
                medicineDopingRuleRepository.findAllByMedicineId(medicine.getId())
                        .stream()
                        .map(MedicineDopingRule::getId)
                        .toList()
        );
        return response;
    }

    private MedicineDopingRuleStatus parseDopingRuleStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new MedicineValidationException(List.of("Status is required"));
        }

        try {
            return MedicineDopingRuleStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new MedicineValidationException(List.of("Invalid status. Allowed values: SAFE, BANNED, RESTRICTED, UNKNOWN"));
        }
    }
}
