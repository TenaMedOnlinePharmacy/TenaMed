package com.TenaMed.ocr.mapper;

import com.TenaMed.ocr.dto.MedicineOcrItem;
import com.TenaMed.ocr.dto.OcrResultDto;
import com.TenaMed.ocr.dto.external.VeryfiOcrResponseDto;

import java.util.List;

public class OcrMapper {

    public OcrResultDto map(VeryfiOcrResponseDto response) {
        if (response == null) {
            return new OcrResultDto(false, 0.0, List.of());
        }

        MedicineOcrItem item = new MedicineOcrItem();
        item.setName(response.getMedicineName());
        item.setQuantity(response.getQuantity());
        item.setInstruction(response.getInstructions());

        return new OcrResultDto(true, 0.0, List.of(item));
    }
}
