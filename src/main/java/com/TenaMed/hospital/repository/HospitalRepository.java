package com.TenaMed.hospital.repository;

import com.TenaMed.hospital.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HospitalRepository extends JpaRepository<Hospital, UUID> {

    Optional<Hospital> findByLicenseNumber(String licenseNumber);

    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);
}
