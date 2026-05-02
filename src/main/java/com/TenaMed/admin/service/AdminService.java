package com.TenaMed.admin.service;

import com.TenaMed.admin.dto.DashboardResponse;
import com.TenaMed.admin.dto.OcrStatsResponse;
import com.TenaMed.user.repository.UserRepository;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
import com.TenaMed.hospital.repository.HospitalRepository;
import com.TenaMed.medicine.repository.ProductRepository;
import com.TenaMed.pharmacy.repository.OrderRepository;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import com.TenaMed.audit.repository.AuditLogRepository;
import com.TenaMed.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.hospital.mapper.HospitalMapper;
import com.TenaMed.pharmacy.mapper.PharmacyMapper;
import com.TenaMed.hospital.dto.HospitalResponseDto;
import com.TenaMed.pharmacy.dto.response.PharmacyResponse;
import com.TenaMed.hospital.entity.HospitalStatus;
import com.TenaMed.pharmacy.enums.PharmacyStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PharmacyRepository pharmacyRepository;
    private final HospitalRepository hospitalRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AuditLogRepository auditLogRepository;
    private final HospitalMapper hospitalMapper;
    private final PharmacyMapper pharmacyMapper;

    public DashboardResponse getDashboard() {
        DashboardResponse response = new DashboardResponse();
        response.setTotalUsers(userRepository.count());
        response.setTotalPharmacies(pharmacyRepository.count());
        response.setTotalHospitals(hospitalRepository.count());
        response.setTotalProducts(productRepository.count());
        response.setTotalOrders(orderRepository.count());
        return response;
    }

    public OcrStatsResponse getOcrStats() {
        OcrStatsResponse response = new OcrStatsResponse();
        response.setTotalProcessed(prescriptionRepository.countByOcrSuccessIsNotNull());
        Double avgConf = prescriptionRepository.getAverageOcrConfidence();
        response.setAvgConfidence(avgConf != null ? avgConf : 0.0);
        response.setPassedCount(prescriptionRepository.countByOcrSuccess(true));
        response.setFailedCount(prescriptionRepository.countByOcrSuccess(false));
        return response;
    }

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    public Page<Prescription> getPrescriptions(String status, Boolean highRisk, Pageable pageable) {
        return prescriptionRepository.findByStatusAndHighRisk(status, highRisk, pageable);
    }

    public List<HospitalResponseDto> getPendingHospitals() {
        return hospitalRepository.findByStatus(HospitalStatus.PENDING).stream()
                .map(hospitalMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<HospitalResponseDto> searchHospitalsByName(String name) {
        return hospitalRepository.findByNameContainingIgnoreCase(name).stream()
                .map(hospitalMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Map<String, Long> getHospitalStats() {
        return Map.of(
                "total", hospitalRepository.count(),
                "verified", hospitalRepository.countByStatus(HospitalStatus.ACTIVE),
                "rejected", hospitalRepository.countByStatus(HospitalStatus.REJECTED),
                "suspended", hospitalRepository.countByStatus(HospitalStatus.SUSPENDED)
        );
    }

    public List<PharmacyResponse> getPendingPharmacies() {
        return pharmacyRepository.findByStatus(PharmacyStatus.PENDING).stream()
                .map(pharmacyMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<PharmacyResponse> searchPharmaciesByName(String name) {
        return pharmacyRepository.findByNameContainingIgnoreCase(name).stream()
                .map(pharmacyMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Map<String, Long> getPharmacyStats() {
        return Map.of(
                "total", pharmacyRepository.count(),
                "verified", pharmacyRepository.countByStatus(PharmacyStatus.VERIFIED),
                "rejected", pharmacyRepository.countByStatus(PharmacyStatus.REJECTED),
                "suspended", pharmacyRepository.countByStatus(PharmacyStatus.SUSPENDED)
        );
    }
}
