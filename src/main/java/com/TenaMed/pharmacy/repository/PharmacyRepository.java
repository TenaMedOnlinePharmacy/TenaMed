package com.TenaMed.pharmacy.repository;

import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.enums.PharmacyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PharmacyRepository extends JpaRepository<Pharmacy, UUID> {

    List<Pharmacy> findByStatus(PharmacyStatus status);

    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);
}