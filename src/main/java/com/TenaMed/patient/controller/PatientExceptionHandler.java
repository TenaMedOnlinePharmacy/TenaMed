package com.TenaMed.patient.controller;

import com.TenaMed.patient.exception.PatientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "com.TenaMed.patient")
public class PatientExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationFailure(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }

    @ExceptionHandler(PatientException.class)
    public ResponseEntity<Map<String, String>> handlePatientExceptions(PatientException ex) {
        HttpStatus status = resolveStatus(ex);
        return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
    }

    private HttpStatus resolveStatus(PatientException ex) {
        String className = ex.getClass().getSimpleName();
        if (className.contains("NotFound")) {
            return HttpStatus.NOT_FOUND;
        }
        if (className.contains("AlreadyExists") || className.contains("Duplicate")) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
