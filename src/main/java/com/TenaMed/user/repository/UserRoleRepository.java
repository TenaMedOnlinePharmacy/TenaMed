package com.TenaMed.user.repository;

import com.TenaMed.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    boolean existsByUser_IdAndRole_Id(UUID userId, UUID roleId);
}
