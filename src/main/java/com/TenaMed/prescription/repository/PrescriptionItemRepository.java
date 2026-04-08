package com.TenaMed.prescription.repository;

import com.TenaMed.prescription.entity.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, UUID> {
}
