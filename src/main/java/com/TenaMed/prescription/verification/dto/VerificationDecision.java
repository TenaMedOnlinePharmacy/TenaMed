package com.TenaMed.prescription.verification.dto;

import com.TenaMed.prescription.verification.enums.ReviewReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationDecision {
    private boolean verified;
    private boolean requiresManualReview;
    private ReviewReason reviewReason;
}
