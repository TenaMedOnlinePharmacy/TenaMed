package com.tenamed.admin.service;

import com.tenamed.admin.dto.DashboardResponse;
import com.TenaMed.user.repository.UserRepository;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
import com.TenaMed.hospital.repository.HospitalRepository;
import com.TenaMed.medicine.repository.ProductRepository;
import com.TenaMed.pharmacy.repository.OrderRepository;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import com.tenamed.admin.dto.OcrStatsResponse;
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
}
