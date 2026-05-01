package com.TenaMed.user.controller;

import com.TenaMed.user.dto.AssignRoleRequestDto;
import com.TenaMed.user.dto.UserDetailsResponseDto;
import com.TenaMed.user.dto.UserRolesResponseDto;
import com.TenaMed.user.service.IdentityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
public class AdminController {

    private final IdentityService identityService;

    public AdminController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDetailsResponseDto> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(identityService.getUserDetails(id));
    }

    @PostMapping("/{id}/roles")
    public ResponseEntity<UserRolesResponseDto> assignRole(@PathVariable UUID id,
                                                           @Valid @RequestBody AssignRoleRequestDto requestDto) {
        return ResponseEntity.ok(identityService.assignRoleToUser(id, requestDto.getRoleName()));
    }

    @DeleteMapping("/{id}/roles/{roleName}")
    public ResponseEntity<UserRolesResponseDto> removeRole(@PathVariable UUID id,
                                                            @PathVariable String roleName) {
        return ResponseEntity.ok(identityService.removeRoleFromUser(id, roleName));
    }

    @GetMapping("/{id}/roles")
    public ResponseEntity<UserRolesResponseDto> getUserRoles(@PathVariable UUID id) {
        return ResponseEntity.ok(identityService.getUserRoles(id));
    }

    @PostMapping("/roles/populate")
    public ResponseEntity<Map<String, Object>> populateRoles() {
        List<String> createdRoles = identityService.populateRoles();
        return ResponseEntity.ok(Map.of(
                "createdRoles", createdRoles,
                "createdCount", createdRoles.size()
        ));
    }
}
