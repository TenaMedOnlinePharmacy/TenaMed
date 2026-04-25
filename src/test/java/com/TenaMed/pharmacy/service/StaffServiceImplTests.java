package com.TenaMed.pharmacy.service;

import com.TenaMed.pharmacy.dto.request.AddStaffRequest;
import com.TenaMed.pharmacy.dto.response.StaffResponse;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.entity.UserPharmacy;
import com.TenaMed.pharmacy.enums.EmploymentStatus;
import com.TenaMed.pharmacy.enums.StaffRole;
import com.TenaMed.pharmacy.exception.StaffAlreadyExistsException;
import com.TenaMed.pharmacy.mapper.UserPharmacyMapper;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
import com.TenaMed.pharmacy.repository.UserPharmacyRepository;
import com.TenaMed.pharmacy.service.impl.StaffServiceImpl;
import com.TenaMed.user.entity.User;
import com.TenaMed.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffServiceImplTests {

    @Mock
    private UserPharmacyRepository userPharmacyRepository;

    @Mock
    private PharmacyRepository pharmacyRepository;

    @Mock
    private UserPharmacyMapper userPharmacyMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StaffServiceImpl staffService;

    @Test
    void shouldAddStaff() {
        UUID pharmacyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AddStaffRequest request = new AddStaffRequest();
        request.setPharmacyId(pharmacyId);
        request.setUserId(userId);
        request.setStaffRole(StaffRole.PHARMACIST);
        request.setEmploymentStatus(EmploymentStatus.ACTIVE);

        Pharmacy pharmacy = new Pharmacy();
        UserPharmacy membership = new UserPharmacy();
        StaffResponse response = StaffResponse.builder().userId(userId).build();

        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
 
        when(pharmacyRepository.findById(pharmacyId)).thenReturn(Optional.of(pharmacy));
        when(userPharmacyRepository.existsByUserIdAndPharmacyId(userId, pharmacyId)).thenReturn(false);
        when(userPharmacyMapper.toEntity(request, pharmacy)).thenReturn(membership);
        when(userPharmacyRepository.save(membership)).thenReturn(membership);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userPharmacyMapper.toResponse(membership, "John", "Doe")).thenReturn(response);

        StaffResponse actual = staffService.addStaff(request);

        assertEquals(userId, actual.getUserId());
    }

    @Test
    void shouldFailWhenStaffAlreadyExists() {
        UUID pharmacyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AddStaffRequest request = new AddStaffRequest();
        request.setPharmacyId(pharmacyId);
        request.setUserId(userId);

        Pharmacy pharmacy = new Pharmacy();
        when(pharmacyRepository.findById(pharmacyId)).thenReturn(Optional.of(pharmacy));
        when(userPharmacyRepository.existsByUserIdAndPharmacyId(userId, pharmacyId)).thenReturn(true);

        assertThrows(StaffAlreadyExistsException.class, () -> staffService.addStaff(request));
    }

    @Test
    void shouldVerifyStaff() {
        UUID pharmacyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID verifier = UUID.randomUUID();
        UserPharmacy membership = new UserPharmacy();
        StaffResponse response = StaffResponse.builder().verifiedBy(verifier).build();

        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
 
        when(userPharmacyRepository.findByUserIdAndPharmacy_Id(userId, pharmacyId)).thenReturn(Optional.of(membership));
        when(userPharmacyRepository.save(any(UserPharmacy.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userPharmacyMapper.toResponse(any(UserPharmacy.class), any(), any())).thenReturn(response);

        StaffResponse actual = staffService.verifyStaff(pharmacyId, userId, verifier);

        assertEquals(verifier, actual.getVerifiedBy());
    }
}