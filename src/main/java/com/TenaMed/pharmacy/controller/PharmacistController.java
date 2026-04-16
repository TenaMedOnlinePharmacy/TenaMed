package com.TenaMed.pharmacy.controller;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.pharmacy.dto.request.PharmacistInviteRegistrationRequestDto;
import com.TenaMed.pharmacy.dto.response.StaffResponse;
import com.TenaMed.pharmacy.service.PharmacistOnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pharmacists")
public class PharmacistController {

    private final PharmacistOnboardingService pharmacistOnboardingService;

    public PharmacistController(PharmacistOnboardingService pharmacistOnboardingService) {
        this.pharmacistOnboardingService = pharmacistOnboardingService;
    }

    @PostMapping("/create")
    public ResponseEntity<StaffResponse> createPharmacistFromInvite(@RequestParam("token") String token,
                                                                     @Valid @RequestBody PharmacistInviteRegistrationRequestDto dto) {
        if (token == null || token.trim().isEmpty()) {
            throw new BadRequestException("token is required");
        }

        StaffResponse response = pharmacistOnboardingService.registerAndCreatePharmacistFromInvite(token, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
