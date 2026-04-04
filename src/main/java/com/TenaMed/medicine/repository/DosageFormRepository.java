package com.TenaMed.medicine.repository;

import com.TenaMed.medicine.entity.DosageForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DosageFormRepository extends JpaRepository<DosageForm, UUID> {

    Optional<DosageForm> findByNameIgnoreCase(String name);
}
