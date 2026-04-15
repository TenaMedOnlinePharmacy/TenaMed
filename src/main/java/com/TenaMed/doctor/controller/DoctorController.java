package com.TenaMed.doctor.controller;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.common.security.CurrentUserProvider;
import com.TenaMed.doctor.dto.DoctorRequestDto;
import com.TenaMed.doctor.dto.DoctorResponseDto;
import com.TenaMed.doctor.service.DoctorService;
import com.TenaMed.invitation.entity.Invitation;
import com.TenaMed.invitation.entity.InvitationRole;
import com.TenaMed.invitation.service.InvitationService;
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
    private final InvitationService invitationService;
    private final CurrentUserProvider currentUserProvider;

    public DoctorController(DoctorService doctorService,
                            InvitationService invitationService,
                            CurrentUserProvider currentUserProvider) {
        this.doctorService = doctorService;
        this.invitationService = invitationService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/")
    public ResponseEntity<DoctorResponseDto> createDoctorFromInvite(@RequestParam("token") String token,
                                                                     @Valid @RequestBody DoctorRequestDto dto) {
        Invitation invitation = invitationService.validateToken(token);
        if (invitation.getRole() != InvitationRole.DOCTOR) {
            throw new BadRequestException("Invitation role does not allow doctor onboarding");
        }

        UUID currentUserId = currentUserProvider.getCurrentUserId();
        DoctorResponseDto response = doctorService.createDoctorFromInvite(currentUserId, invitation.getHospitalId(), dto);
        invitationService.markAsAccepted(token);
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
