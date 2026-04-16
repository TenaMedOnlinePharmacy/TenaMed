package com.TenaMed.pharmacy.service.impl;

import com.TenaMed.pharmacy.dto.request.AddStaffRequest;
import com.TenaMed.pharmacy.dto.response.StaffResponse;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.entity.UserPharmacy;
import com.TenaMed.pharmacy.exception.PharmacyNotFoundException;
import com.TenaMed.pharmacy.exception.StaffAlreadyExistsException;
import com.TenaMed.pharmacy.exception.UserPharmacyNotFoundException;
import com.TenaMed.pharmacy.mapper.UserPharmacyMapper;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
import com.TenaMed.pharmacy.repository.UserPharmacyRepository;
import com.TenaMed.pharmacy.service.StaffService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StaffServiceImpl implements StaffService {

    private final UserPharmacyRepository userPharmacyRepository;
    private final PharmacyRepository pharmacyRepository;
    private final UserPharmacyMapper userPharmacyMapper;

    public StaffServiceImpl(UserPharmacyRepository userPharmacyRepository,
                            PharmacyRepository pharmacyRepository,
                            UserPharmacyMapper userPharmacyMapper) {
        this.userPharmacyRepository = userPharmacyRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.userPharmacyMapper = userPharmacyMapper;
    }

    @Override
    public StaffResponse addStaff(AddStaffRequest request) {
        Pharmacy pharmacy = pharmacyRepository.findById(request.getPharmacyId())
            .orElseThrow(() -> new PharmacyNotFoundException(request.getPharmacyId()));

        if (userPharmacyRepository.existsByUserIdAndPharmacyId(request.getUserId(), request.getPharmacyId())) {
            throw new StaffAlreadyExistsException(request.getUserId(), request.getPharmacyId());
        }

        UserPharmacy staff = userPharmacyMapper.toEntity(request, pharmacy);
        return userPharmacyMapper.toResponse(userPharmacyRepository.save(staff));
    }

    @Override
    public StaffResponse verifyStaff(UUID pharmacyId, UUID userId, UUID verifiedBy) {
        UserPharmacy staff = userPharmacyRepository.findByUserIdAndPharmacy_Id(userId, pharmacyId)
            .orElseThrow(() -> new UserPharmacyNotFoundException(userId));
        staff.setVerifiedBy(verifiedBy);
        staff.setVerifiedAt(LocalDateTime.now());
        return userPharmacyMapper.toResponse(userPharmacyRepository.save(staff));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffResponse> listStaff(UUID pharmacyId) {
        return userPharmacyRepository.findByPharmacyId(pharmacyId)
            .stream()
            .map(userPharmacyMapper::toResponse)
            .toList();
    }
}