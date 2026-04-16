package com.TenaMed.doctor.controller;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.common.security.CurrentUserProvider;
import com.TenaMed.doctor.dto.DoctorInviteRegistrationRequestDto;
import com.TenaMed.doctor.dto.DoctorResponseDto;
import com.TenaMed.doctor.service.DoctorOnboardingService;
import com.TenaMed.doctor.service.DoctorService;
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

    public DoctorController(DoctorService doctorService,
                            DoctorOnboardingService doctorOnboardingService,
                            CurrentUserProvider currentUserProvider) {
        this.doctorService = doctorService;
        this.doctorOnboardingService = doctorOnboardingService;
        this.currentUserProvider = currentUserProvider;
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

    @PatchMapping("/{id}/verify")
    public ResponseEntity<DoctorResponseDto> verifyDoctor(@PathVariable UUID id) {
        return ResponseEntity.ok(doctorService.verifyDoctor(id));
    }
}
