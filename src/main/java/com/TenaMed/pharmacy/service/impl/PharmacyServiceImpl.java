package com.TenaMed.pharmacy.service.impl;

import com.TenaMed.pharmacy.dto.request.CreatePharmacyRequest;
import com.TenaMed.pharmacy.dto.response.PharmacyResponse;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.enums.PharmacyStatus;
import com.TenaMed.pharmacy.exception.PharmacyNotFoundException;
import com.TenaMed.pharmacy.mapper.PharmacyMapper;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
import com.TenaMed.pharmacy.service.PharmacyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class PharmacyServiceImpl implements PharmacyService {

    private final PharmacyRepository pharmacyRepository;
    private final PharmacyMapper pharmacyMapper;

    public PharmacyServiceImpl(PharmacyRepository pharmacyRepository,
                               PharmacyMapper pharmacyMapper) {
        this.pharmacyRepository = pharmacyRepository;
        this.pharmacyMapper = pharmacyMapper;
    }

    @Override
    public PharmacyResponse createPharmacy(CreatePharmacyRequest request) {
        Pharmacy pharmacy = pharmacyMapper.toEntity(request);
        Pharmacy saved = pharmacyRepository.save(pharmacy);
        return pharmacyMapper.toResponse(saved);
    }

    @Override
    public PharmacyResponse verifyPharmacy(UUID pharmacyId, UUID verifiedBy) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
            .orElseThrow(() -> new PharmacyNotFoundException(pharmacyId));
        pharmacy.setStatus(PharmacyStatus.VERIFIED);
        pharmacy.setVerifiedBy(verifiedBy);
        pharmacy.setVerifiedAt(LocalDateTime.now());
        return pharmacyMapper.toResponse(pharmacyRepository.save(pharmacy));
    }

    @Override
    @Transactional(readOnly = true)
    public PharmacyResponse getPharmacy(UUID pharmacyId) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
            .orElseThrow(() -> new PharmacyNotFoundException(pharmacyId));
        return pharmacyMapper.toResponse(pharmacy);
    }
}