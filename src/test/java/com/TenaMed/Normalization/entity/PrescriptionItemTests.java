package com.TenaMed.Normalization.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrescriptionItemTests {

    @Test
    void shouldDefaultQuantityToOneWhenNullOnCreate() {
        PrescriptionItem item = new PrescriptionItem();
        item.setQuantity(null);

        item.applyDefaults();

        assertEquals(1, item.getQuantity());
    }

    @Test
    void shouldKeepQuantityWhenProvidedOnCreate() {
        PrescriptionItem item = new PrescriptionItem();
        item.setQuantity(4);

        item.applyDefaults();

        assertEquals(4, item.getQuantity());
    }
}