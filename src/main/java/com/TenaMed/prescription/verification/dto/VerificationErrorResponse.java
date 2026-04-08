package com.TenaMed.prescription.verification.dto;

import java.time.LocalDateTime;

public record VerificationErrorResponse(String error, LocalDateTime timestamp) {
}
