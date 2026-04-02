package com.TenaMed.ocr.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private String patientName;
    private MetaDto meta;
    private List<PrescriptionItemDto> prescriptionList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class MetaDto {
        private List<PageDto> pages;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class PageDto {
        private Double ocrScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class PrescriptionItemDto {
        private String prescriptionName;
        private String prescriptionDose;
        private String prescriptionDescription;
    }
}
