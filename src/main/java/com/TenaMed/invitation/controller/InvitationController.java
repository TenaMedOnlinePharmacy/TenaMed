package com.TenaMed.invitation.controller;

import com.TenaMed.invitation.dto.InvitationResponseDto;
import com.TenaMed.invitation.service.InvitationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<InvitationResponseDto> getInvitationByToken(@PathVariable String token) {
        return ResponseEntity.ok(invitationService.getByToken(token));
    }
}
