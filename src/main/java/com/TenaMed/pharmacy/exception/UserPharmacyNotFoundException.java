package com.TenaMed.pharmacy.exception;

import java.util.UUID;

public class UserPharmacyNotFoundException extends PharmacyException {

    public UserPharmacyNotFoundException(UUID userPharmacyId) {
        super("Staff membership not found with id: " + userPharmacyId);
    }
}