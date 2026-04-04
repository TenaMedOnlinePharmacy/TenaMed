package com.TenaMed.medicine.validator;

import com.TenaMed.medicine.dto.MedicineRequestDto;
import com.TenaMed.medicine.exception.MedicineValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MedicineValidator {

    public void validate(MedicineRequestDto dto) {
        List<String> errors = new ArrayList<>();

        if (dto.getName() == null || dto.getName().isBlank()) {
            errors.add("Medicine name must not be blank");
        }

        if (dto.getCategory() == null || dto.getCategory().isBlank()) {
            errors.add("Category must not be blank");
        }

        if (dto.getDosageForm() == null || dto.getDosageForm().isBlank()) {
            errors.add("Dosage form must not be blank");
        }

        if (dto.getNeedManualReview() == null) {
            errors.add("Manual review flag must be provided");
        }

        if (dto.getPregnancyCategory() != null && dto.getPregnancyCategory().length() > 1) {
            errors.add("Pregnancy category must be a single character");
        }

        if (!errors.isEmpty()) {
            throw new MedicineValidationException(errors);
        }
    }
}
