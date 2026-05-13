package com.TenaMed.complaint.controller;

import com.TenaMed.complaint.exception.ComplaintAccessDeniedException;
import com.TenaMed.complaint.exception.ComplaintException;
import com.TenaMed.complaint.exception.ComplaintNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "com.TenaMed.complaint")
public class ComplaintExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationFailure(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }

    @ExceptionHandler(ComplaintException.class)
    public ResponseEntity<Map<String, String>> handleComplaintExceptions(ComplaintException ex) {
        HttpStatus status = resolveStatus(ex);
        return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
    }

    private HttpStatus resolveStatus(ComplaintException ex) {
        if (ex instanceof ComplaintNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (ex instanceof ComplaintAccessDeniedException) {
            return HttpStatus.FORBIDDEN;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
