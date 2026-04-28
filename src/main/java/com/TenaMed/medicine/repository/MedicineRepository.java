package com.TenaMed.medicine.repository;

import com.TenaMed.medicine.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, UUID>,
        JpaSpecificationExecutor<Medicine> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Medicine> findByNameIgnoreCase(String name);

    Optional<Medicine> findFirstByNameIgnoreCaseOrGenericNameIgnoreCase(String name, String genericName);

    List<Medicine> findByNameContainingIgnoreCaseOrGenericNameContainingIgnoreCase(String nameKeyword, String genericNameKeyword);
}
