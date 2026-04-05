package com.TenaMed.user.repository;

import com.TenaMed.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"account", "userRoles", "userRoles.role"})
    Optional<User> findByAccount_Id(UUID accountId);

    @EntityGraph(attributePaths = {"account", "userRoles", "userRoles.role"})
    Optional<User> findByAccountId(UUID accountId);

    boolean existsByPhone(String phone);
}
