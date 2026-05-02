package com.TenaMed.hospital.repository;

import com.TenaMed.hospital.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface HospitalRepository extends JpaRepository<Hospital, UUID> {

    Optional<Hospital> findByLicenseNumber(String licenseNumber);

    Optional<Hospital> findByOwnerId(UUID ownerId);

    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);

    List<Hospital> findByStatus(com.TenaMed.hospital.entity.HospitalStatus status);

    List<Hospital> findByNameContainingIgnoreCase(String name);

    long countByStatus(com.TenaMed.hospital.entity.HospitalStatus status);
}
