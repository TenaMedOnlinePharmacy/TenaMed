package com.TenaMed.admin.controller;

import com.TenaMed.user.dto.AdminPharmacistRequestDto;
import com.TenaMed.user.dto.AdminPharmacistResponseDto;
import com.TenaMed.user.service.IdentityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/admin-pharmacists")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPharmacistController {

    private final IdentityService identityService;

    @PostMapping
    public ResponseEntity<AdminPharmacistResponseDto> createAdminPharmacist(
            @Valid @RequestBody AdminPharmacistRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(identityService.createAdminPharmacist(requestDto));
    }
}
