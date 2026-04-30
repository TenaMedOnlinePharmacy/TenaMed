package com.TenaMed.hospital.controller;

import com.TenaMed.common.security.CurrentUserProvider;
import com.TenaMed.doctor.dto.DoctorResponseDto;
import com.TenaMed.doctor.entity.DoctorStatus;
import com.TenaMed.hospital.dto.HospitalDoctorResponseDto;
import com.TenaMed.hospital.dto.HospitalRequestDto;
import com.TenaMed.hospital.dto.HospitalResponseDto;
import com.TenaMed.hospital.dto.HospitalStatisticsDto;
import com.TenaMed.hospital.entity.Hospital;
import com.TenaMed.hospital.repository.HospitalRepository;
import com.TenaMed.hospital.service.HospitalService;
import com.TenaMed.invitation.dto.DoctorInvitationRequestDto;
import com.TenaMed.invitation.dto.InvitationResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospitals")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;
    private final CurrentUserProvider currentUserProvider;
    private final HospitalRepository hospitalRepository;

    @PostMapping("/")
    public ResponseEntity<HospitalResponseDto> createHospital(@Valid @RequestBody HospitalRequestDto dto) {
        HospitalResponseDto response = hospitalService.createHospital(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HospitalResponseDto> getHospitalById(@PathVariable UUID id) {
        return ResponseEntity.ok(hospitalService.getHospitalById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HospitalResponseDto> updateHospital(@PathVariable UUID id,
                                                              @Valid @RequestBody HospitalRequestDto dto) {
        return ResponseEntity.ok(hospitalService.updateHospital(id, dto));
    }

    @PatchMapping("/{id}/verify")
    public ResponseEntity<HospitalResponseDto> verifyHospital(@PathVariable UUID id) {
        return ResponseEntity.ok(hospitalService.verifyHospital(id));
    }

    @GetMapping("/{id}/doctors")
    public ResponseEntity<List<DoctorResponseDto>> getHospitalDoctors(@PathVariable UUID id) {
        return ResponseEntity.ok(hospitalService.getHospitalDoctors(id));
    }

    @PostMapping("/invite-doctor")
    public ResponseEntity<InvitationResponseDto> inviteDoctor(@Valid @RequestBody DoctorInvitationRequestDto request) {
        UUID ownerId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(hospitalService.inviteDoctorForOwner(ownerId, request.getEmail()));
    }

    @GetMapping("/statistics")
    public ResponseEntity<HospitalStatisticsDto> getHospitalStatistics() {
        UUID ownerId = currentUserProvider.getCurrentUserId();
        Optional<Hospital> hospital = hospitalRepository.findByOwnerId(ownerId);
        return hospital.map(value -> ResponseEntity.ok(hospitalService.getHospitalStatistics(value.getId(), ownerId))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/doctors/management")
    public ResponseEntity<List<HospitalDoctorResponseDto>> getHospitalDoctorsForManagement() {
        UUID ownerId = currentUserProvider.getCurrentUserId();
        Optional<Hospital> hospital = hospitalRepository.findByOwnerId(ownerId);
        return hospital.map(value -> ResponseEntity.ok(hospitalService.getHospitalDoctorsByStatus(value.getId(), List.of(DoctorStatus.ACTIVE, DoctorStatus.PENDING))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/doctors/{doctorId}/accept")
    public ResponseEntity<Void> acceptDoctor(@PathVariable UUID doctorId) {
        UUID ownerId = currentUserProvider.getCurrentUserId();
        hospitalService.acceptDoctor(doctorId, ownerId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/doctors/{doctorId}/reject")
    public ResponseEntity<Void> rejectDoctor(@PathVariable UUID doctorId) {
        UUID ownerId = currentUserProvider.getCurrentUserId();
        hospitalService.rejectDoctor(doctorId, ownerId);
        return ResponseEntity.ok().build();
    }
}
