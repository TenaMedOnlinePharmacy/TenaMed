package com.TenaMed.ocr.mapper;

import com.TenaMed.ocr.dto.MedicineOcrItem;
import com.TenaMed.ocr.dto.OcrResultDto;
import com.TenaMed.ocr.dto.external.VeryfiOcrResponseDto;
import com.TenaMed.prescription.entity.Prescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class OcrMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public VeryfiOcrResponseDto mapToVeryfiResponse(String responseBody) throws JsonProcessingException {
        return objectMapper.readValue(responseBody, VeryfiOcrResponseDto.class);
    }

    public OcrResultDto map(VeryfiOcrResponseDto response, Prescription prescriptions) {
        if (response == null) {
            return new OcrResultDto(false, 0.0, List.of(), prescriptions);
        }

        List<MedicineOcrItem> medicines = new ArrayList<>();

        if (response.getPrescriptionList() != null && !response.getPrescriptionList().isEmpty()) {
            for (VeryfiOcrResponseDto.PrescriptionItemDto prescription : response.getPrescriptionList()) {
                if (prescription == null) {
                    continue;
                }

                MedicineOcrItem item = new MedicineOcrItem();
                item.setName(prescription.getPrescriptionName());
                item.setQuantity(parseQuantity(prescription.getPrescriptionDose()));
                item.setInstruction(prescription.getPrescriptionDescription());
                medicines.add(item);
            }
        }

        if (medicines.isEmpty()) {
            MedicineOcrItem item = new MedicineOcrItem();
            item.setName(response.getMedicineName());
            item.setQuantity(response.getQuantity());
            item.setInstruction(response.getInstructions());
            medicines.add(item);
        }

        double confidence = extractConfidence(response);

        boolean hasUsableMedicine = medicines.stream().anyMatch(item ->
                (item.getName() != null && !item.getName().isBlank())
                        || item.getQuantity() != null
                        || (item.getInstruction() != null && !item.getInstruction().isBlank())
        );

        if (!hasUsableMedicine) {
            return new OcrResultDto(false, confidence, List.of(), prescriptions);
        }

        return new OcrResultDto(true, confidence, medicines, prescriptions);
    }

    private double extractConfidence(VeryfiOcrResponseDto response) {
        if (response.getMeta() == null || response.getMeta().getPages() == null || response.getMeta().getPages().isEmpty()) {
            return 0.0;
        }

        Double ocrScore = response.getMeta().getPages().get(0).getOcrScore();
        return ocrScore == null ? 0.0 : ocrScore;
    }

    private Integer parseQuantity(String rawDose) {
        if (rawDose == null || rawDose.isBlank()) {
            return null;
        }

        String digits = rawDose.replaceAll("\\D+", "");
        if (digits.isBlank()) {
            return null;
        }

        try {
            return Integer.valueOf(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

}
