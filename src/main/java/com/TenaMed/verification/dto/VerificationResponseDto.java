package com.TenaMed.verification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponseDto {
	private String status;
	private String reason;
	private String nextAction;
}
