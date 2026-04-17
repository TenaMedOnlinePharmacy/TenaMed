package com.TenaMed.antidoping.repository;

import com.TenaMed.antidoping.entity.AthleteProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AthleteProfileRepository extends JpaRepository<AthleteProfile, UUID> {

    boolean existsByUserId(UUID userId);
}
