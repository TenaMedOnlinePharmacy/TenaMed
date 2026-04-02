package com.TenaMed.payment;

import com.TenaMed.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Pattern TX_REF_PATTERN = Pattern.compile("\\\"tx_ref\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern STATUS_PATTERN = Pattern.compile("\\\"status\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
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
    public ResponseEntity<Map<String, String>> initialize(@RequestBody Map<String, String> request) {
        try {
            String checkoutUrl = paymentService.initializePayment(
                    request.get("amount"),
                    request.get("email"),
                    request.get("firstName"),
                    request.get("lastName"),
                    request.get("phone")
            );
            return ResponseEntity.ok(Map.of("checkout_url", checkoutUrl == null ? "" : checkoutUrl));
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("checkout_url", ""));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody String payload) {
        try {
            String txRef = extractTxRef(payload);
            if (txRef == null || txRef.isBlank()) {
                System.out.println("PAYMENT FAILED");
                return ResponseEntity.ok("PAYMENT FAILED");
            }

            String verificationResponse = paymentService.verifyPayment(txRef);
            String status = extractStatus(verificationResponse);

            if ("success".equalsIgnoreCase(status)) {
                System.out.println("PAYMENT SUCCESS");
            } else {
                System.out.println("PAYMENT FAILED");
            }

            return ResponseEntity.ok("OK");
        } catch (IOException ex) {
            System.out.println("PAYMENT FAILED");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ERROR");
        }
    }

    private String extractTxRef(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }
        Matcher matcher = TX_REF_PATTERN.matcher(rawJson);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractStatus(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }
        Matcher matcher = STATUS_PATTERN.matcher(rawJson);
        return matcher.find() ? matcher.group(1) : null;
    }
}
