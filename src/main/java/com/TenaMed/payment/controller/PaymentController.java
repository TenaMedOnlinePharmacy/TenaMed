package com.TenaMed.payment.controller;

import com.TenaMed.payment.dto.CancelPaymentResponse;
import com.TenaMed.payment.dto.PaymentRequest;
import com.TenaMed.payment.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
        this.objectMapper = new ObjectMapper();
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testInitialize() {
        try {
            String checkoutUrl = paymentService.initializePayment(
                    "100",
                    "test@tenamed.com",
                    "Test",
                    "User",
                    "0911000000"
            );
            return ResponseEntity.ok(Map.of("checkout_url", checkoutUrl == null ? "" : checkoutUrl));
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("checkout_url", ""));
        }
    }

    @PostMapping("/initialize")
    public ResponseEntity<Map<String, String>> initialize(@RequestBody PaymentRequest request) {
        try {
            String checkoutUrl = paymentService.initializePayment(
                    request.getAmount(),
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getPhone()
            );
            return ResponseEntity.ok(Map.of("checkout_url", checkoutUrl == null ? "erro2" : checkoutUrl));
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("checkout_url", "error"));
        }
    }

    @PutMapping("/cancel/{txRef}")
    public ResponseEntity<CancelPaymentResponse> cancel(@PathVariable String txRef) {
        return ResponseEntity.ok(paymentService.cancelPayment(txRef));
    }

    @RequestMapping(value = "/webhook", method = {RequestMethod.POST, RequestMethod.GET})
    //this methode response must be saved in database
    public ResponseEntity<String> webhook(
            @RequestBody(required = false) String payload,
            @RequestParam(required = false) String tx_ref
    ) {

        try {
            String txRef = tx_ref;

            if (txRef == null && payload != null) {
                txRef = extractTxRef(payload);
            }


            if (txRef == null || txRef.isBlank()) {
                System.out.println("PAYMENT FAILED");
                return ResponseEntity.ok("FAILED");
            }

            String verificationResponse = paymentService.verifyPayment(txRef);

            if (isSuccessStatus(verificationResponse)) {
                System.out.println("PAYMENT SUCCESS");
            } else {
                System.out.println("PAYMENT FAILED");
            }

            return ResponseEntity.ok("OK");

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ERROR");
        }
    }

    private String extractTxRef(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            String trxRef = root.path("trx_ref").asText(null);
            if (trxRef != null && !trxRef.isBlank()) {
                return trxRef;
            }

            return null;

        } catch (IOException ex) {
            return null;
        }
    }

    private boolean isSuccessStatus(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return false;
        }

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            String nestedStatus = root.path("data").path("status").asText("");
            if ("success".equalsIgnoreCase(nestedStatus)) {
                return true;
            }

            String topLevelStatus = root.path("status").asText("");
            return "success".equalsIgnoreCase(topLevelStatus);
        } catch (IOException ex) {
            return false;
        }
    }
}
