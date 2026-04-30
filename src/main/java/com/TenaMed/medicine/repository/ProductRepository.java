package com.TenaMed.medicine.repository;

import com.TenaMed.medicine.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByMedicineId(UUID medicineId);
    Optional<Product> findFirstByMedicineId(UUID medicineId);
    Optional<Product> findByBrandNameAndManufacturer(String brandName, String manufacturer);

    List<Product> findByMedicineIdIn(Set<UUID> uuids);
}
