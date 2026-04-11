package com.TenaMed.pharmacy.exception;

import java.util.UUID;

public class PharmacyNotFoundException extends PharmacyException {

    public PharmacyNotFoundException(UUID pharmacyId) {
        super("Pharmacy not found with id: " + pharmacyId);
    }
}