package com.TenaMed.pharmacy.exception;

import java.util.UUID;

public class StaffAlreadyExistsException extends PharmacyException {

    public StaffAlreadyExistsException(UUID userId, UUID pharmacyId) {
        super("Staff already exists for user " + userId + " in pharmacy " + pharmacyId);
    }
}