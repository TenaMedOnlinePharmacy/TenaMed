package com.TenaMed.patient.repository;

import com.TenaMed.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
	boolean existsByUniqueCode(String uniqueCode);

	Optional<Patient> findByUniqueCodeAndPhone(String uniqueCode, String phone);
}
