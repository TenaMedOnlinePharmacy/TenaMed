package com.TenaMed.medicine.repository;

import com.TenaMed.medicine.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByMedicineIdIn(Collection<UUID> medicineIds);
}
