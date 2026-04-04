package com.TenaMed.medicine.specification;

import com.TenaMed.medicine.entity.Medicine;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class MedicineSpecification {

    private MedicineSpecification() {}

    public static Specification<Medicine> hasKeyword(String keyword) {
        return (root, query, cb) ->
                !StringUtils.hasText(keyword)
                        ? null
                        : cb.or(
                                cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"),
                                cb.like(cb.lower(root.get("genericName")), "%" + keyword.toLowerCase() + "%")
                        );
    }

    public static Specification<Medicine> hasCategoryName(String categoryName) {
        return (root, query, cb) ->
                !StringUtils.hasText(categoryName)
                        ? null
                        : cb.equal(cb.lower(root.join("category").get("name")), categoryName.toLowerCase());
    }

    public static Specification<Medicine> hasTherapeuticClass(String therapeuticClass) {
        return (root, query, cb) ->
                !StringUtils.hasText(therapeuticClass)
                        ? null
                        : cb.equal(cb.lower(root.get("therapeuticClass")), therapeuticClass.toLowerCase());
    }

    public static Specification<Medicine> requiresPrescription(Boolean requiresPrescription) {
        return (root, query, cb) ->
                requiresPrescription == null ? null : cb.equal(root.get("requiresPrescription"), requiresPrescription);
    }

    public static Specification<Medicine> hasDosageFormName(String dosageFormName) {
        return (root, query, cb) ->
                !StringUtils.hasText(dosageFormName)
                        ? null
                        : cb.equal(cb.lower(root.join("dosageForm").get("name")), dosageFormName.toLowerCase());
    }
}
