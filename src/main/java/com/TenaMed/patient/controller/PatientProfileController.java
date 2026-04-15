package com.TenaMed.patient.controller;

import com.TenaMed.patient.dto.CreateProfileDto;
import com.TenaMed.patient.dto.CreatePatientDto;
import com.TenaMed.patient.dto.PatientProfileResponse;
import com.TenaMed.patient.dto.UpdateProfileDto;
import com.TenaMed.patient.service.PatientService;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/patient")
public class PatientProfileController {

    private final PatientService patientService;

    public PatientProfileController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping("/profile")
    public ResponseEntity<?> createProfile(Principal principal,
                                           @Valid @RequestBody CreateProfileDto dto) {
        UUID userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        PatientProfileResponse response = patientService.createProfile(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Principal principal) {
        UUID userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        return ResponseEntity.ok(patientService.getProfileByUserId(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(Principal principal,
                                           @Valid @RequestBody UpdateProfileDto dto) {
        UUID userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        return ResponseEntity.ok(patientService.updateProfile(userId, dto));
    }

    @PostMapping("/convert/{patientId}")
    public ResponseEntity<?> convertTemporaryPatient(@PathVariable UUID patientId,
                                                     Principal principal) {
        UUID userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        patientService.convertTemporaryPatient(patientId, userId);
        return ResponseEntity.ok(Map.of("message", "Temporary patient converted successfully"));
    }

    @PostMapping("/temporary")
    public ResponseEntity<?> createTemporaryPatient(@Valid @RequestBody CreatePatientDto dto,
                                                    Principal principal) {
        UUID userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.createTemporaryPatient(dto));
    }

    @DeleteMapping("/temporary/{patientId}")
    public ResponseEntity<?> deleteTemporaryPatient(@PathVariable UUID patientId,
                                                    Principal principal) {
        UUID userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        patientService.deleteTemporaryPatient(patientId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveUserId(Principal principal) {
        AuthenticatedUserPrincipal authenticatedUserPrincipal = resolveAuthenticatedPrincipal(principal);
        if (authenticatedUserPrincipal != null) {
            return authenticatedUserPrincipal.getUserId();
        }
        return null;
    }

    private AuthenticatedUserPrincipal resolveAuthenticatedPrincipal(Principal principal) {
        if (principal instanceof AuthenticatedUserPrincipal directPrincipal) {
            return directPrincipal;
        }
        Authentication authentication = resolveAuthentication(principal);
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal nestedPrincipal) {
            return nestedPrincipal;
        }
        return null;
    }

    private Authentication resolveAuthentication(Principal principal) {
        if (principal instanceof Authentication authentication) {
            return authentication;
        }
        Authentication contextAuth = SecurityContextHolder.getContext().getAuthentication();
        if (contextAuth != null && contextAuth.isAuthenticated()) {
            return contextAuth;
        }
        return null;
    }
}
