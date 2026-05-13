package com.TenaMed.complaint.repository;

import com.TenaMed.complaint.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {

    List<Complaint> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);

    List<Complaint> findAllByOrderByCreatedAtDesc();

    Optional<Complaint> findByIdAndCustomer_Id(UUID complaintId, UUID customerId);
}
