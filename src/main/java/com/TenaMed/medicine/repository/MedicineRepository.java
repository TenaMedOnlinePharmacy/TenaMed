package com.TenaMed.medicine.repository;

import com.TenaMed.medicine.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long>,
        JpaSpecificationExecutor<Medicine> {

    boolean existsByNameIgnoreCase(String name);
}
