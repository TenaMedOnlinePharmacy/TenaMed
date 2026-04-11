package com.TenaMed.pharmacy.repository;

import com.TenaMed.pharmacy.entity.Order;
import com.TenaMed.pharmacy.entity.OrderItem;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.entity.UserPharmacy;
import com.TenaMed.pharmacy.enums.EmploymentStatus;
import com.TenaMed.pharmacy.enums.OrderStatus;
import com.TenaMed.pharmacy.enums.PaymentStatus;
import com.TenaMed.pharmacy.enums.PharmacyStatus;
import com.TenaMed.pharmacy.enums.StaffRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class PharmacyRepositoryDataJpaTests {

    @Autowired
    private PharmacyRepository pharmacyRepository;

    @Autowired
    private UserPharmacyRepository userPharmacyRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    void pharmacyRepository_shouldFindByStatus() {
        Pharmacy pending = createPharmacy("Pending Pharmacy", "LIC-PENDING", PharmacyStatus.PENDING);
        createPharmacy("Verified Pharmacy", "LIC-VERIFIED", PharmacyStatus.VERIFIED);

        List<Pharmacy> results = pharmacyRepository.findByStatus(PharmacyStatus.PENDING);

        assertEquals(1, results.size());
        assertEquals(pending.getId(), results.getFirst().getId());
    }

    @Test
    void userPharmacyRepository_shouldSupportQueryMethods() {
        Pharmacy pharmacy = createPharmacy("Central Pharmacy", "LIC-UP-1", PharmacyStatus.VERIFIED);
        UUID userId = UUID.randomUUID();

        UserPharmacy userPharmacy = new UserPharmacy();
        userPharmacy.setUserId(userId);
        userPharmacy.setPharmacy(pharmacy);
        userPharmacy.setStaffRole(StaffRole.PHARMACIST);
        userPharmacy.setEmploymentStatus(EmploymentStatus.ACTIVE);
        userPharmacyRepository.save(userPharmacy);

        List<UserPharmacy> byUser = userPharmacyRepository.findByUserId(userId);
        List<UserPharmacy> byPharmacy = userPharmacyRepository.findByPharmacyId(pharmacy.getId());
        boolean exists = userPharmacyRepository.existsByUserIdAndPharmacyId(userId, pharmacy.getId());
        boolean notExists = userPharmacyRepository.existsByUserIdAndPharmacyId(UUID.randomUUID(), pharmacy.getId());

        assertEquals(1, byUser.size());
        assertEquals(1, byPharmacy.size());
        assertEquals(userPharmacy.getId(), byUser.getFirst().getId());
        assertEquals(userPharmacy.getId(), byPharmacy.getFirst().getId());
        assertTrue(exists);
        assertFalse(notExists);
    }

    @Test
    void orderRepository_shouldSupportQueryMethods() {
        Pharmacy pharmacy = createPharmacy("Order Pharmacy", "LIC-OR-1", PharmacyStatus.VERIFIED);
        UUID customerId = UUID.randomUUID();

        Order order = new Order();
        order.setCustomerId(customerId);
        order.setPharmacy(pharmacy);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalAmount(new BigDecimal("120.00"));
        Order savedOrder = orderRepository.save(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(savedOrder);
        orderItem.setInventoryId(UUID.randomUUID());
        orderItem.setMedicineId(UUID.randomUUID());
        orderItem.setQuantity(3);
        orderItem.setUnitPrice(new BigDecimal("40.00"));
        orderItemRepository.save(orderItem);

        List<Order> byCustomer = orderRepository.findByCustomerId(customerId);
        List<Order> byPharmacy = orderRepository.findByPharmacyId(pharmacy.getId());
        List<Order> byStatus = orderRepository.findByStatus(OrderStatus.ACCEPTED);

        assertEquals(1, byCustomer.size());
        assertEquals(1, byPharmacy.size());
        assertEquals(1, byStatus.size());
        assertEquals(savedOrder.getId(), byCustomer.getFirst().getId());
        assertEquals(savedOrder.getId(), byPharmacy.getFirst().getId());
        assertEquals(savedOrder.getId(), byStatus.getFirst().getId());
    }

    private Pharmacy createPharmacy(String name, String license, PharmacyStatus status) {
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setName(name);
        pharmacy.setLicenseNumber(license);
        pharmacy.setEmail(name.toLowerCase().replace(" ", "") + "@test.com");
        pharmacy.setPhone("+251900000000");
        pharmacy.setOwnerId(UUID.randomUUID());
        pharmacy.setStatus(status);
        return pharmacyRepository.save(pharmacy);
    }
}