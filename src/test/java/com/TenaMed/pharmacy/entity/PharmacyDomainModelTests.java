package com.TenaMed.pharmacy.entity;

import com.TenaMed.pharmacy.enums.EmploymentStatus;
import com.TenaMed.pharmacy.enums.OrderStatus;
import com.TenaMed.pharmacy.enums.PaymentStatus;
import com.TenaMed.pharmacy.enums.PharmacyStatus;
import com.TenaMed.pharmacy.enums.StaffRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PharmacyDomainModelTests {

    @Test
    void shouldCreateEntitiesAndWireRelationships() {
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setName("Tena Pharmacy");
        pharmacy.setLicenseNumber("LIC-1234");
        pharmacy.setEmail("pharmacy@tenamed.com");
        pharmacy.setPhone("+2348000000000");
        pharmacy.setOwnerId(UUID.randomUUID());
        pharmacy.setStatus(PharmacyStatus.PENDING);

        UserPharmacy userPharmacy = new UserPharmacy();
        userPharmacy.setUserId(UUID.randomUUID());
        userPharmacy.setPharmacy(pharmacy);
        userPharmacy.setStaffRole(StaffRole.PHARMACIST);
        userPharmacy.setEmploymentStatus(EmploymentStatus.ACTIVE);

        Order order = new Order();
        order.setCustomerId(UUID.randomUUID());
        order.setPharmacy(pharmacy);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalAmount(new BigDecimal("50.00"));

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setInventoryId(UUID.randomUUID());
        item.setMedicineId(UUID.randomUUID());
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("25.00"));

        pharmacy.getUserPharmacies().add(userPharmacy);
        pharmacy.getOrders().add(order);
        order.getItems().add(item);

        assertEquals("Tena Pharmacy", pharmacy.getName());
        assertEquals(PharmacyStatus.PENDING, pharmacy.getStatus());
        assertEquals(1, pharmacy.getUserPharmacies().size());
        assertEquals(1, pharmacy.getOrders().size());
        assertEquals(1, order.getItems().size());
        assertTrue(pharmacy.getOrders().contains(order));
        assertTrue(order.getItems().contains(item));
    }
}