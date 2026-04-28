package com.TenaMed.doctor.controller;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.common.exception.UnauthorizedException;
import com.TenaMed.common.security.CurrentUserProvider;
import com.TenaMed.doctor.dto.CreateDoctorPrescriptionRequestDto;
import com.TenaMed.doctor.dto.CreateDoctorPrescriptionResponseDto;
import com.TenaMed.doctor.dto.DoctorInviteRegistrationRequestDto;
import com.TenaMed.doctor.dto.DoctorResponseDto;
import com.TenaMed.doctor.dto.VerifyDoctorRequestDto;
import com.TenaMed.doctor.entity.DoctorStatus;
import com.TenaMed.doctor.service.DoctorOnboardingService;
import com.TenaMed.doctor.service.DoctorService;
import com.TenaMed.patient.dto.CreatePatientDto;
import com.TenaMed.patient.dto.PatientDto;
import com.TenaMed.patient.service.PatientService;
import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.service.PrescriptionService;
import jakarta.validation.Valid;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final DoctorOnboardingService doctorOnboardingService;
    private final CurrentUserProvider currentUserProvider;
    private final PatientService patientService;
    private final PrescriptionService prescriptionService;

    public DoctorController(DoctorService doctorService,
                            DoctorOnboardingService doctorOnboardingService,
                            CurrentUserProvider currentUserProvider,
                            PatientService patientService,
                            PrescriptionService prescriptionService) {
        this.doctorService = doctorService;
        this.doctorOnboardingService = doctorOnboardingService;
        this.currentUserProvider = currentUserProvider;
        this.patientService = patientService;
        this.prescriptionService = prescriptionService;
    }

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
}
