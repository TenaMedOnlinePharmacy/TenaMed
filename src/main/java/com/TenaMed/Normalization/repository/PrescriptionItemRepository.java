package com.TenaMed.Normalization.repository;

import com.TenaMed.Normalization.entity.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, UUID> {

	@Modifying
	@Query("""
			DELETE FROM PrescriptionItem pi
			WHERE pi.prescription.id = :prescriptionId
			""")
	void deleteByPrescriptionId(@Param("prescriptionId") UUID prescriptionId);
}
