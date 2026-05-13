package com.TenaMed.complaint.service.impl;

import com.TenaMed.complaint.dto.request.CreateComplaintRequest;
import com.TenaMed.complaint.dto.response.ComplaintResponse;
import com.TenaMed.complaint.entity.Complaint;
import com.TenaMed.complaint.enums.ComplaintStatus;
import com.TenaMed.complaint.exception.ComplaintAccessDeniedException;
import com.TenaMed.complaint.exception.ComplaintNotFoundException;
import com.TenaMed.complaint.exception.ComplaintValidationException;
import com.TenaMed.complaint.repository.ComplaintRepository;
import com.TenaMed.complaint.service.ComplaintService;
import com.TenaMed.pharmacy.entity.Order;
import com.TenaMed.pharmacy.repository.OrderRepository;
import com.TenaMed.user.entity.User;
import com.TenaMed.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    public ComplaintResponse createComplaint(UUID customerId, CreateComplaintRequest request) {
        if (customerId == null) {
            throw new ComplaintValidationException("Customer id is required");
        }
        if (request == null) {
            throw new ComplaintValidationException("Complaint payload is required");
        }
        if (request.getOrderId() == null) {
            throw new ComplaintValidationException("orderId is required");
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ComplaintValidationException("Customer not found"));

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ComplaintValidationException("Order not found with id: " + request.getOrderId()));

        if (!customerId.equals(order.getCustomerId())) {
            throw new ComplaintAccessDeniedException("You are not allowed to create a complaint for this order");
        }

        Complaint complaint = new Complaint();
        complaint.setCustomer(customer);
        complaint.setOrder(order);
        complaint.setCategory(request.getCategory());
        complaint.setSubject(request.getSubject().trim());
        complaint.setDescription(request.getDescription().trim());
        complaint.setStatus(ComplaintStatus.PENDING);

        Complaint saved = complaintRepository.save(complaint);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getCustomerComplaints(UUID customerId) {
        if (customerId == null) {
            throw new ComplaintValidationException("Customer id is required");
        }
        return complaintRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintResponse getCustomerComplaint(UUID customerId, UUID complaintId) {
        if (customerId == null) {
            throw new ComplaintValidationException("Customer id is required");
        }
        if (complaintId == null) {
            throw new ComplaintValidationException("Complaint id is required");
        }
        Complaint complaint = complaintRepository.findByIdAndCustomer_Id(complaintId, customerId)
                .orElseThrow(() -> new ComplaintNotFoundException(complaintId));
        return toResponse(complaint);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getAllComplaints() {
        return complaintRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintResponse getComplaint(UUID complaintId) {
        if (complaintId == null) {
            throw new ComplaintValidationException("Complaint id is required");
        }
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ComplaintNotFoundException(complaintId));
        return toResponse(complaint);
    }

    @Override
    public ComplaintResponse updateStatus(UUID complaintId, ComplaintStatus status) {
        if (complaintId == null) {
            throw new ComplaintValidationException("Complaint id is required");
        }
        if (status == null) {
            throw new ComplaintValidationException("Status is required");
        }

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ComplaintNotFoundException(complaintId));
        complaint.setStatus(status);
        Complaint saved = complaintRepository.save(complaint);
        return toResponse(saved);
    }

    @Override
    public ComplaintResponse addAdminNote(UUID complaintId, String adminNote) {
        if (complaintId == null) {
            throw new ComplaintValidationException("Complaint id is required");
        }
        if (adminNote == null || adminNote.isBlank()) {
            throw new ComplaintValidationException("Admin note is required");
        }

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ComplaintNotFoundException(complaintId));
        complaint.setAdminNote(adminNote.trim());
        Complaint saved = complaintRepository.save(complaint);
        return toResponse(saved);
    }

    private ComplaintResponse toResponse(Complaint complaint) {
        return ComplaintResponse.builder()
                .id(complaint.getId())
                .customerId(complaint.getCustomer() != null ? complaint.getCustomer().getId() : null)
                .orderId(complaint.getOrder() != null ? complaint.getOrder().getId() : null)
                .category(complaint.getCategory())
                .subject(complaint.getSubject())
                .description(complaint.getDescription())
                .status(complaint.getStatus())
                .adminNote(complaint.getAdminNote())
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .build();
    }
}
