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
import com.TenaMed.events.DomainEventService;
import com.TenaMed.user.entity.User;
import com.TenaMed.user.exception.UserNotFoundException;
import com.TenaMed.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class StaffServiceImpl implements StaffService {

    private final UserPharmacyRepository userPharmacyRepository;
    private final PharmacyRepository pharmacyRepository;
    private final UserPharmacyMapper userPharmacyMapper;
    private final DomainEventService domainEventService;
    private final UserRepository userRepository;

    public StaffServiceImpl(UserPharmacyRepository userPharmacyRepository,
                            PharmacyRepository pharmacyRepository,
                            UserPharmacyMapper userPharmacyMapper,
                            DomainEventService domainEventService,
                            UserRepository userRepository) {
        this.userPharmacyRepository = userPharmacyRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.userPharmacyMapper = userPharmacyMapper;
        this.domainEventService = domainEventService;
        this.userRepository = userRepository;
    }

    @Override
    public StaffResponse addStaff(AddStaffRequest request) {
        Pharmacy pharmacy = pharmacyRepository.findById(request.getPharmacyId())
            .orElseThrow(() -> new PharmacyNotFoundException(request.getPharmacyId()));

        if (userPharmacyRepository.existsByUserIdAndPharmacyId(request.getUserId(), request.getPharmacyId())) {
            throw new StaffAlreadyExistsException(request.getUserId(), request.getPharmacyId());
        }

        UserPharmacy staff = userPharmacyMapper.toEntity(request, pharmacy);
        UserPharmacy saved = userPharmacyRepository.save(staff);
        domainEventService.publish(
            "PHARMACY_STAFF_ADDED",
            "USER_PHARMACY",
            saved.getId(),
            "PHARMACY_OWNER",
            pharmacy.getOwnerId(),
            "PHARMACY",
            pharmacy.getId(),
            Map.of("userId", saved.getUserId().toString(), "staffRole", String.valueOf(saved.getStaffRole()))
        );
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new UserNotFoundException(request.getUserId()));
        return userPharmacyMapper.toResponse(saved, user.getFirstName(), user.getLastName());
    }

    @Override
    public StaffResponse verifyStaff(UUID pharmacyId, UUID userId, UUID verifiedBy) {
        UserPharmacy staff = userPharmacyRepository.findByUserIdAndPharmacy_Id(userId, pharmacyId)
            .orElseThrow(() -> new UserPharmacyNotFoundException(userId));
        staff.setVerifiedBy(verifiedBy);
        staff.setVerifiedAt(LocalDateTime.now());
        staff.setIsVerified(true);
        UserPharmacy saved = userPharmacyRepository.save(staff);
        domainEventService.publish(
                "PHARMACY_STAFF_VERIFIED",
                "USER_PHARMACY",
                saved.getId(),
                "PHARMACY_OWNER",
                verifiedBy,
                "PHARMACY",
                pharmacyId,
                Map.of("userId", saved.getUserId().toString())
        );
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        return userPharmacyMapper.toResponse(saved, user.getFirstName(), user.getLastName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffResponse> listStaff(UUID pharmacyId) {
        List<UserPharmacy> staffRecords = userPharmacyRepository.findByPharmacyId(pharmacyId);
        List<UUID> userIds = staffRecords.stream().map(UserPharmacy::getUserId).toList();
        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

        return staffRecords.stream()
            .map(record -> {
                User user = userMap.get(record.getUserId());
                String firstName = user != null ? user.getFirstName() : null;
                String lastName = user != null ? user.getLastName() : null;
                return userPharmacyMapper.toResponse(record, firstName, lastName);
            })
            .toList();
    }
}