package com.TenaMed.doctor.controller;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.common.exception.UnauthorizedException;
import com.TenaMed.common.exception.ResourceNotFoundException;
import com.TenaMed.common.security.CurrentUserProvider;
import com.TenaMed.doctor.dto.CreateDoctorPrescriptionRequestDto;
import com.TenaMed.doctor.dto.CreateDoctorPrescriptionResponseDto;
import com.TenaMed.doctor.dto.DoctorInviteRegistrationRequestDto;
import com.TenaMed.doctor.dto.DoctorAssignedPrescriptionResponseDto;
import com.TenaMed.doctor.dto.EditDoctorPrescriptionItemsRequestDto;
import com.TenaMed.doctor.dto.DoctorResponseDto;
import com.TenaMed.doctor.dto.DoctorAssignedPrescriptionItemResponseDto;
import com.TenaMed.doctor.dto.VerifyDoctorRequestDto;
import com.TenaMed.doctor.entity.DoctorStatus;
import com.TenaMed.doctor.repository.DoctorRepository;
import com.TenaMed.doctor.service.DoctorOnboardingService;
import com.TenaMed.doctor.service.DoctorService;
import com.TenaMed.patient.dto.CreatePatientDto;
import com.TenaMed.patient.dto.PatientDto;
import com.TenaMed.patient.entity.Patient;
import com.TenaMed.patient.repository.PatientRepository;
import com.TenaMed.patient.service.PatientService;
import com.TenaMed.Normalization.entity.PrescriptionItem;
import com.TenaMed.Normalization.repository.PrescriptionItemRepository;
import com.TenaMed.medicine.repository.MedicineRepository;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final DoctorRepository doctorRepository;
    private final DoctorOnboardingService doctorOnboardingService;
    private final CurrentUserProvider currentUserProvider;
    private final PatientService patientService;
    private final PrescriptionService prescriptionService;
    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final MedicineRepository medicineRepository;



    @PostMapping("/create")
    public ResponseEntity<DoctorResponseDto> createDoctorFromInvite(@RequestParam("token") String token,
                                                                     @Valid @RequestBody DoctorInviteRegistrationRequestDto dto) {
        if (token == null || token.trim().isEmpty()) {
            throw new BadRequestException("token is required");
        }

        DoctorResponseDto response = doctorOnboardingService.registerAndCreateDoctorFromInvite(token, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<DoctorResponseDto> getMyProfile() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(doctorService.getMyProfile(currentUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorResponseDto> getDoctorById(@PathVariable UUID id) {
        return ResponseEntity.ok(doctorService.getDoctorById(id));
    }

    @PatchMapping("/verify")
    public ResponseEntity<DoctorResponseDto> verifyDoctor(@Valid @RequestBody VerifyDoctorRequestDto request) {
        UUID ownerId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(doctorService.verifyDoctorForOwner(ownerId, request.getDoctorId()));
    }

    @PostMapping("/prescriptions")
    public ResponseEntity<CreateDoctorPrescriptionResponseDto> createPrescriptionForPatient(@Valid @RequestBody CreateDoctorPrescriptionRequestDto request) {
        if (!currentUserProvider.hasRole("DOCTOR")) {
            throw new UnauthorizedException("Doctor role is required");
        }
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        DoctorResponseDto doctor = doctorService.getMyProfile(currentUserId);
        if (doctor.getStatus() != DoctorStatus.ACTIVE) {
            throw new BadRequestException("Only active doctors can create prescriptions");
        }

        PatientDto patient = patientService.createTemporaryPatient(
            new CreatePatientDto(request.getFullName(), request.getPhone(), request.getUniqueCode())
        );

        Prescription prescription = prescriptionService.createDoctorPrescription(
            patient.id(),
            doctor.getHospitalId(),
            doctor.getId(),
            request.getExpiryDate(),
            request.getMaxRefillsAllowed(),
            request.getUniqueCode(),
            request.getHighRisk(),
            request.getType(),
            request.getItems()
        );

        CreateDoctorPrescriptionResponseDto response = CreateDoctorPrescriptionResponseDto.builder()
            .patientId(patient.id())
            .prescriptionId(prescription.getId())
            .doctorId(doctor.getId())
            .hospitalId(doctor.getHospitalId())
            .issueDate(prescription.getIssueDate())
            .expiryDate(prescription.getExpiryDate())
            .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/prescriptions")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DoctorAssignedPrescriptionResponseDto>> getPrescriptionsAssignedToDoctor() {
        if (!currentUserProvider.hasRole("DOCTOR")) {
            throw new UnauthorizedException("Doctor role is required");
        }

        UUID userID = currentUserProvider.getCurrentUserId();
        UUID doctorId = doctorRepository.getDoctorByUserId(userID).getId();
        List<Prescription> prescriptions = prescriptionRepository.findByDoctorId(doctorId);

        List<DoctorAssignedPrescriptionResponseDto> response = prescriptions.stream()
            .map(this::toDoctorAssignedPrescriptionResponseDto)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/prescriptions/{prescriptionId}")
    @Transactional
    public ResponseEntity<DoctorAssignedPrescriptionResponseDto> editDoctorPrescriptionItems(
        @PathVariable UUID prescriptionId,
        @Valid @RequestBody EditDoctorPrescriptionItemsRequestDto request
    ) {
        if (!currentUserProvider.hasRole("DOCTOR")) {
            throw new UnauthorizedException("Doctor role is required");
        }
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("items are required");
        }

        UUID doctorId = currentUserProvider.getCurrentUserId();
        Prescription prescription = prescriptionRepository.findByIdAndDoctorId(prescriptionId, doctorId)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + prescriptionId));

        prescriptionItemRepository.deleteByPrescriptionId(prescriptionId);

        List<PrescriptionItem> itemsToSave = request.getItems().stream().map(item -> {
            var medicine = medicineRepository.findById(item.getMedicineId())
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + item.getMedicineId()));

            PrescriptionItem prescriptionItem = new PrescriptionItem();
            prescriptionItem.setPrescription(prescription);
            prescriptionItem.setMedicine(medicine);
            prescriptionItem.setQuantity(item.getQuantity());
            prescriptionItem.setForm(item.getForm());
            prescriptionItem.setInstructions(item.getInstruction());
            prescriptionItem.setStrength(item.getStrength());
            return prescriptionItem;
        }).collect(Collectors.toList());

        prescriptionItemRepository.saveAll(itemsToSave);
        DoctorAssignedPrescriptionResponseDto updated = toDoctorAssignedPrescriptionResponseDto(prescription);
        return ResponseEntity.ok(updated);
    }

    private DoctorAssignedPrescriptionResponseDto toDoctorAssignedPrescriptionResponseDto(Prescription prescription) {
        String patientFullName = null;
        if (prescription.getPatientId() != null) {
            Patient patient = patientRepository.findById(prescription.getPatientId()).orElse(null);
            patientFullName = patient != null ? patient.getFullName() : null;
        }

        List<PrescriptionItem> prescriptionItems = prescriptionItemRepository.findByPrescriptionId(prescription.getId());
        List<DoctorAssignedPrescriptionItemResponseDto> items = prescriptionItems.stream()
            .map(item -> new DoctorAssignedPrescriptionItemResponseDto(
                item.getMedicine() != null ? item.getMedicine().getId() : null,
                item.getQuantity(),
                item.getMedicine() != null ? item.getMedicine().getName() : null,
                item.getForm(),
                item.getInstructions(),
                item.getStrength()
            ))
            .collect(Collectors.toList());

        return new DoctorAssignedPrescriptionResponseDto(
            prescription.getId(),
            prescription.getUniqueCode(),
            patientFullName,
            prescription.getExpiryDate(),
            items
        );

    }
}
