package com.TenaMed.payment.service;

import com.TenaMed.payment.ChapaHttpClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PaymentService {

    private static final Pattern CHECKOUT_URL_PATTERN = Pattern.compile("\\\"checkout_url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private final ChapaHttpClient chapaHttpClient;

    public PaymentService(ChapaHttpClient chapaHttpClient) {
        this.chapaHttpClient = chapaHttpClient;
    }

    public String initializePayment(
            String amount,
            String email,
            String firstName,
            String lastName,
            String phoneNumber
    ) throws IOException {
        String txRef = UUID.randomUUID().toString();

        String jsonBody = "{" +
                "\"amount\":\"" + escapeJson(amount) + "\"," +
                "\"currency\":\"ETB\"," +
                "\"email\":\"" + escapeJson(email) + "\"," +
                "\"first_name\":\"" + escapeJson(firstName) + "\"," +
                "\"last_name\":\"" + escapeJson(lastName) + "\"," +
                "\"phone_number\":\"" + escapeJson(phoneNumber) + "\"," +
                "\"tx_ref\":\"" + txRef + "\"," +
                "\"customization[title]\":\"TenaMed Payment\"," +
                "\"customization[description]\":\"Prescription Payment\"" +
                "}";

        String rawResponse = chapaHttpClient.initializeTransaction(jsonBody);
        return extractCheckoutUrl(rawResponse);
    }

    public String verifyPayment(String txRef) throws IOException {
        return chapaHttpClient.verifyTransaction(txRef);
    }

    private String extractCheckoutUrl(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return null;
        }

        Matcher matcher = CHECKOUT_URL_PATTERN.matcher(rawResponse);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
