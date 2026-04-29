package com.TenaMed.invitation.repository;

import com.TenaMed.invitation.entity.Invitation;
import com.TenaMed.invitation.entity.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByToken(String token);

    long countByHospitalIdAndStatus(UUID hospitalId, InvitationStatus status);
}
