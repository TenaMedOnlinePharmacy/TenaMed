package com.TenaMed.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalStatisticsDto {
    private Long totalDoctors;
    private Long verifiedDoctors;
    private Long unverifiedDoctors;
    private Long invitedDoctors;
    private Long totalPrescriptions;
}
