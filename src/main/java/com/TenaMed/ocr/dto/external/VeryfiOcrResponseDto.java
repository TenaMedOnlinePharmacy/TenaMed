package com.TenaMed.ocr.dto.external;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VeryfiOcrResponseDto {
    private Long id;
    private String createdDate;
    private String updatedDate;
    private String medicineName;
    private String rxNumber;
    private String date;
    private String expirationDate;
    private Integer quantity;
    private String instructions;
    private String consumerName;
    private String externalId;
}
