package com.TenaMed.payment.controller;

import com.TenaMed.payment.dto.CancelPaymentResponse;
import com.TenaMed.payment.dto.PaymentRequest;
import com.TenaMed.payment.dto.PaymentWebhookResponse;
import com.TenaMed.payment.service.PaymentService;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

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
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("checkout_url", "Use POST /api/payments/initialize"));
    }

    @PostMapping("/initialize")
    public ResponseEntity<Map<String, String>> initialize(@RequestBody PaymentRequest request,
                                                          @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        System.out.println("here");
        try {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("checkout_url", "error"));
            }

            String checkoutUrl = paymentService.initializePayment(request.getOrderId(), principal.getUserId());
            return ResponseEntity.ok(Map.of("checkout_url", checkoutUrl == null ? "erro2" : checkoutUrl));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("checkout_url", "error"));
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("checkout_url", "error"));
        }
    }

    @PutMapping("/cancel/{txRef}")
    public ResponseEntity<CancelPaymentResponse> cancel(@PathVariable String txRef) {
        return ResponseEntity.ok(paymentService.cancelPayment(txRef));
    }

    @GetMapping("/status/{orderId}")
    public ResponseEntity<Map<String, Object>> checkStatus(@PathVariable UUID orderId) {
        try {
            return ResponseEntity.ok(paymentService.getPaymentStatusByOrderId(orderId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @RequestMapping(value = "/webhook", method = {RequestMethod.POST, RequestMethod.GET})
        public ResponseEntity<PaymentWebhookResponse> webhook(
            @RequestBody(required = false) String payload,
            @RequestParam(required = false) String tx_ref
    ) {
        try {
            String txRef = tx_ref;

            if (txRef == null && payload != null) {
                txRef = extractTxRef(payload);
            }


            if (txRef == null || txRef.isBlank()) {
                return ResponseEntity.badRequest().body(new PaymentWebhookResponse(
                        "tx_ref is required",
                        "failed",
                        null,
                        null,
                        null,
                        null,
                        null
                ));
            }

            String verificationResponse = paymentService.verifyPayment(txRef);
            boolean success = isSuccessStatus(verificationResponse);
            PaymentWebhookResponse response = paymentService.processWebhook(txRef, success);
            System.out.println(response);
            if (!success) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new PaymentWebhookResponse(
                    "Unable to process webhook",
                    "error",
                    null,
                    null,
                    null,
                    null,
                    null
            ));
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
