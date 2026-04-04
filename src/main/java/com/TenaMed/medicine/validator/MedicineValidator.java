package com.TenaMed.medicine.validator;

import com.TenaMed.medicine.dto.MedicineRequestDto;
import com.TenaMed.medicine.exception.MedicineValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class MedicineValidator {

    public void validate(MedicineRequestDto dto) {
        List<String> errors = new ArrayList<>();

        if (dto.getName() == null || dto.getName().isBlank()) {
            errors.add("Medicine name must not be blank");
        }

        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Price must be greater than zero");
        }

        if (dto.getCategory() == null || dto.getCategory().isBlank()) {
            errors.add("Category must not be blank");
        }

        if (dto.getManufacturer() == null || dto.getManufacturer().isBlank()) {
            errors.add("Manufacturer must not be blank");
        }

        if (dto.getStockQuantity() == null || dto.getStockQuantity() < 0) {
            errors.add("Stock quantity must be zero or greater");
        }

        if (!errors.isEmpty()) {
            throw new MedicineValidationException(errors);
        }
    }
}
