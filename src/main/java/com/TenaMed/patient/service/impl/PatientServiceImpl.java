package com.TenaMed.patient.service.impl;

import com.TenaMed.patient.dto.AddAllergyDto;
import com.TenaMed.patient.dto.CreatePatientDto;
import com.TenaMed.patient.dto.CreateProfileDto;
import com.TenaMed.patient.dto.PatientDto;
import com.TenaMed.patient.dto.PatientProfileResponse;
import com.TenaMed.patient.dto.UpdateAllergyDto;
import com.TenaMed.patient.dto.UpdateProfileDto;
import com.TenaMed.medicine.entity.Allergen;
import com.TenaMed.medicine.repository.AllergenRepository;
import com.TenaMed.patient.entity.CustomerAllergy;
import com.TenaMed.patient.entity.Patient;
import com.TenaMed.patient.entity.PatientProfile;
import com.TenaMed.patient.exception.AllergenNotFoundException;
import com.TenaMed.patient.exception.CustomerAllergyNotFoundException;
import com.TenaMed.patient.exception.DuplicateAllergyException;
import com.TenaMed.patient.exception.DuplicateUniqueCodeException;
import com.TenaMed.patient.exception.PatientException;
import com.TenaMed.patient.exception.PatientProfileAlreadyExistsException;
import com.TenaMed.patient.exception.PatientProfileNotFoundException;
import com.TenaMed.patient.exception.TemporaryPatientNotFoundException;
import com.TenaMed.patient.mapper.PatientMapper;
import com.TenaMed.patient.repository.CustomerAllergyRepository;
import com.TenaMed.patient.repository.PatientProfileRepository;
import com.TenaMed.patient.repository.PatientRepository;
import com.TenaMed.patient.service.PatientService;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientProfileRepository patientProfileRepository;
    private final PatientRepository patientRepository;
    private final AllergenRepository allergenRepository;
    private final CustomerAllergyRepository customerAllergyRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PatientMapper patientMapper;

    public PatientServiceImpl(PatientProfileRepository patientProfileRepository,
                              PatientRepository patientRepository,
                              AllergenRepository allergenRepository,
                              CustomerAllergyRepository customerAllergyRepository,
                              PrescriptionRepository prescriptionRepository,
                              PatientMapper patientMapper) {
        this.patientProfileRepository = patientProfileRepository;
        this.patientRepository = patientRepository;
        this.allergenRepository = allergenRepository;
        this.customerAllergyRepository = customerAllergyRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.patientMapper = patientMapper;
    }

    @Override
    @Transactional
    public PatientProfileResponse createProfile(UUID userId, CreateProfileDto dto) {
        validateUserId(userId);
        if (dto == null) {
            throw new PatientException("Profile creation payload is required");
        }
        validateUniqueCodeForCreate(dto.getUniqueCode());

        if (patientProfileRepository.existsByUserId(userId)) {
            throw new PatientProfileAlreadyExistsException(userId);
        }

        PatientProfile profile = new PatientProfile();
        profile.setUserId(userId);
        applyProfileFields(profile, dto.getDateOfBirth(), dto.getGender(), dto.getWeight(), dto.getHeight(),
            dto.getIsPregnant(), dto.getBloodType(), dto.getUniqueCode());

        PatientProfile saved = patientProfileRepository.save(profile);
        return patientMapper.toProfileResponse(saved, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public PatientProfileResponse getProfileByUserId(UUID userId) {
        PatientProfile profile = getProfileEntityByUserId(userId);
        List<CustomerAllergy> allergies = customerAllergyRepository.findByProfile_Id(profile.getId());
        return patientMapper.toProfileResponse(profile, allergies);
    }

    @Override
    @Transactional
    public PatientProfileResponse updateProfile(UUID userId, UpdateProfileDto dto) {
        if (dto == null) {
            throw new PatientException("Profile update payload is required");
        }
        validateUniqueCodeForUpdate(dto.getUniqueCode(), userId);

        PatientProfile profile = getProfileEntityByUserId(userId);

        applyProfileFields(profile, dto.getDateOfBirth(), dto.getGender(), dto.getWeight(), dto.getHeight(),
            dto.getIsPregnant(), dto.getBloodType(), dto.getUniqueCode());

        PatientProfile saved = patientProfileRepository.save(profile);
        List<CustomerAllergy> allergies = customerAllergyRepository.findByProfile_Id(saved.getId());
        return patientMapper.toProfileResponse(saved, allergies);
    }

    @Override
    @Transactional
    public PatientProfileResponse addAllergy(UUID userId, AddAllergyDto dto) {
        if (dto == null || dto.getAllergenId() == null) {
            throw new PatientException("allergenId is required");
        }

        PatientProfile profile = getProfileEntityByUserId(userId);
        Allergen allergen = allergenRepository.findById(dto.getAllergenId())
                .orElseThrow(() -> new AllergenNotFoundException(dto.getAllergenId()));

        boolean exists = customerAllergyRepository.existsByProfile_IdAndAllergen_Id(profile.getId(), allergen.getId());
        if (exists) {
            throw new DuplicateAllergyException();
        }

        CustomerAllergy customerAllergy = new CustomerAllergy();
        customerAllergy.setProfile(profile);
        customerAllergy.setAllergen(allergen);
        customerAllergy.setSeverity(dto.getSeverity());
        customerAllergyRepository.save(customerAllergy);

        List<CustomerAllergy> allergies = customerAllergyRepository.findByProfile_Id(profile.getId());
        return patientMapper.toProfileResponse(profile, allergies);
    }

    @Override
    @Transactional
    public PatientProfileResponse updateAllergy(UUID userId, UUID allergyId, UpdateAllergyDto dto) {
        if (allergyId == null) {
            throw new PatientException("allergyId is required");
        }
        if (dto == null) {
            throw new PatientException("Allergy update payload is required");
        }

        PatientProfile profile = getProfileEntityByUserId(userId);
        CustomerAllergy allergy = customerAllergyRepository.findByIdAndProfile_UserId(allergyId, userId)
                .orElseThrow(() -> new CustomerAllergyNotFoundException(allergyId));

        if (dto.allergenId() != null && !dto.allergenId().equals(allergy.getAllergen().getId())) {
            Allergen allergen = allergenRepository.findById(dto.allergenId())
                    .orElseThrow(() -> new AllergenNotFoundException(dto.allergenId()));

            boolean exists = customerAllergyRepository.existsByProfile_IdAndAllergen_Id(profile.getId(), allergen.getId());
            if (exists) {
                throw new DuplicateAllergyException();
            }
            allergy.setAllergen(allergen);
        }

        allergy.setSeverity(dto.severity());
        customerAllergyRepository.save(allergy);

        List<CustomerAllergy> allergies = customerAllergyRepository.findByProfile_Id(profile.getId());
        return patientMapper.toProfileResponse(profile, allergies);
    }

    @Override
    @Transactional
    public void removeAllergy(UUID userId, UUID allergyId) {
        if (allergyId == null) {
            throw new PatientException("allergyId is required");
        }

        getProfileEntityByUserId(userId);

        CustomerAllergy allergy = customerAllergyRepository.findByIdAndProfile_UserId(allergyId, userId)
                .orElseThrow(() -> new CustomerAllergyNotFoundException(allergyId));
        customerAllergyRepository.delete(allergy);
    }

    @Override
    @Transactional
    public void convertTemporaryPatient(UUID patientId, UUID userId) {
        validateUserId(userId);
        if (patientId == null) {
            throw new PatientException("patientId is required");
        }

        Patient temporaryPatient = patientRepository.findById(patientId)
                .orElseThrow(() -> new TemporaryPatientNotFoundException(patientId));

        if (patientProfileRepository.existsByUserId(userId)) {
            throw new PatientProfileAlreadyExistsException(userId);
        }

        PatientProfile profile = new PatientProfile();
        profile.setUserId(userId);
        PatientProfile savedProfile = patientProfileRepository.save(profile);

        prescriptionRepository.migratePatientToProfile(temporaryPatient.getId(), savedProfile.getId());
    }

    @Override
    @Transactional
    public PatientDto createTemporaryPatient(CreatePatientDto dto) {
        if (dto == null) {
            throw new PatientException("Temporary patient payload is required");
        }

        String fullName = requireText(dto.fullName(), "fullName");
        String phone = requireText(dto.phone(), "phone");
        String uniqueCode = normalizeUniqueCode(dto.uniqueCode());

        if (uniqueCode != null && patientRepository.existsByUniqueCode(uniqueCode)) {
            throw new DuplicateUniqueCodeException(uniqueCode);
        }

        Patient patient = new Patient();
        patient.setFullName(fullName);
        patient.setPhone(phone);
        patient.setUniqueCode(uniqueCode);

        Patient saved = patientRepository.save(patient);
        return new PatientDto(saved.getId(), saved.getFullName(), saved.getPhone(), saved.getUniqueCode(), saved.getCreatedAt());
    }

    @Override
    @Transactional
    public void deleteTemporaryPatient(UUID patientId) {
        if (patientId == null) {
            throw new PatientException("patientId is required");
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new TemporaryPatientNotFoundException(patientId));
        patientRepository.delete(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientProfileResponse.AllergyItem> getAllergies(UUID userId) {
        PatientProfileResponse response = getProfileByUserId(userId);
        return response.getAllergies();
    }

    private PatientProfile getProfileEntityByUserId(UUID userId) {
        validateUserId(userId);
        return patientProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new PatientProfileNotFoundException(userId));
    }

    private void validateUserId(UUID userId) {
        if (userId == null) {
            throw new PatientException("userId is required");
        }
    }

    private void applyProfileFields(PatientProfile profile,
                                    java.time.LocalDate dateOfBirth,
                                    String gender,
                                    Float weight,
                                    Integer height,
                                    Boolean isPregnant,
                                    String bloodType,
                                    String uniqueCode) {
        profile.setDateOfBirth(dateOfBirth);
        profile.setGender(gender);
        profile.setWeight(weight);
        profile.setHeight(height);
        profile.setIsPregnant(isPregnant);
        profile.setBloodType(bloodType);
        profile.setUniqueCode(normalizeUniqueCode(uniqueCode));
    }

    private void validateUniqueCodeForCreate(String uniqueCode) {
        String normalized = normalizeUniqueCode(uniqueCode);
        if (normalized == null) {
            return;
        }
        if (patientProfileRepository.existsByUniqueCode(normalized)) {
            throw new DuplicateUniqueCodeException(uniqueCode);
        }
    }

    private void validateUniqueCodeForUpdate(String uniqueCode, UUID userId) {
        String normalized = normalizeUniqueCode(uniqueCode);
        if (normalized == null) {
            return;
        }
        if (patientProfileRepository.existsByUniqueCodeAndUserIdNot(normalized, userId)) {
            throw new DuplicateUniqueCodeException(uniqueCode);
        }
    }

    private String normalizeUniqueCode(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new PatientException(fieldName + " is required");
        }
        return value.trim();
    }
}
