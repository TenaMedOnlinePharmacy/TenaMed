package com.TenaMed.Normalization.repository;

import com.TenaMed.Normalization.entity.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, UUID> {
}
