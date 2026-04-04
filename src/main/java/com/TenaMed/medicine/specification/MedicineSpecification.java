package com.TenaMed.medicine.specification;

import com.TenaMed.medicine.entity.Medicine;
import org.springframework.data.jpa.domain.Specification;

public class MedicineSpecification {

    private MedicineSpecification() {}

    public static Specification<Medicine> hasName(String name) {
        return (root, query, cb) ->
                name == null ? null : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Medicine> hasCategory(String category) {
        return (root, query, cb) ->
                category == null ? null : cb.equal(cb.lower(root.get("category")), category.toLowerCase());
    }

    public static Specification<Medicine> hasManufacturer(String manufacturer) {
        return (root, query, cb) ->
                manufacturer == null ? null : cb.equal(cb.lower(root.get("manufacturer")), manufacturer.toLowerCase());
    }

    public static Specification<Medicine> requiresPrescription(Boolean requiresPrescription) {
        return (root, query, cb) ->
                requiresPrescription == null ? null : cb.equal(root.get("requiresPrescription"), requiresPrescription);
    }

    public static Specification<Medicine> isInStock() {
        return (root, query, cb) -> cb.greaterThan(root.get("stockQuantity"), 0);
    }
}
