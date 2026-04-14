package com.TenaMed.verification.controller;

import com.TenaMed.verification.dto.VerificationErrorResponse;
import com.TenaMed.verification.exception.InvalidVerificationStateException;
import com.TenaMed.verification.exception.PrescriptionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice(basePackages = "com.TenaMed.verification")
public class VerificationExceptionHandler {

    @ExceptionHandler(PrescriptionNotFoundException.class)
    public ResponseEntity<VerificationErrorResponse> handleNotFound(PrescriptionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new VerificationErrorResponse(ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler({InvalidVerificationStateException.class, IllegalArgumentException.class})
    public ResponseEntity<VerificationErrorResponse> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new VerificationErrorResponse(ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<VerificationErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new VerificationErrorResponse("Unexpected error occurred", LocalDateTime.now()));
    }
}
