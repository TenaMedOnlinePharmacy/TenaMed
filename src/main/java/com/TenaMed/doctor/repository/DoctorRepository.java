package com.TenaMed.doctor.repository;

import com.TenaMed.doctor.entity.Doctor;
import com.TenaMed.doctor.entity.DoctorStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    Optional<Doctor> findByUserId(UUID userId);

    List<Doctor> findByHospitalId(UUID hospitalId);

    boolean existsByUserId(UUID userId);

    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);

    Doctor getDoctorByUserId(UUID userID);

    long countByHospitalId(UUID hospitalId);

    long countByHospitalIdAndStatus(UUID hospitalId, DoctorStatus status);
}
