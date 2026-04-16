package com.TenaMed.pharmacy.service;

import com.TenaMed.pharmacy.dto.request.AddStaffRequest;
import com.TenaMed.pharmacy.dto.response.StaffResponse;

import java.util.List;
import java.util.UUID;

public interface StaffService {

    StaffResponse addStaff(AddStaffRequest request);

    StaffResponse verifyStaff(UUID pharmacyId, UUID userId, UUID verifiedBy);

    List<StaffResponse> listStaff(UUID pharmacyId);
}