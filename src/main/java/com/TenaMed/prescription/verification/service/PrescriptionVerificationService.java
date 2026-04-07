package com.TenaMed.prescription.verification.service;

import com.TenaMed.prescription.verification.dto.VerificationResponseDto;

import java.util.UUID;

public interface PrescriptionVerificationService {
	VerificationResponseDto verify(UUID prescriptionId);
}
