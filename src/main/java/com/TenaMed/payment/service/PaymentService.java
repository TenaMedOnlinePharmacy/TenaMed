package com.TenaMed.payment.service;

import com.TenaMed.payment.ChapaHttpClient;
import com.TenaMed.payment.dto.CancelPaymentData;
import com.TenaMed.payment.dto.CancelPaymentResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@Service
public class PaymentService {

    private final ChapaHttpClient chapaHttpClient;
    private final ObjectMapper objectMapper;

    public PaymentService(ChapaHttpClient chapaHttpClient) {
        this.chapaHttpClient = chapaHttpClient;
        this.objectMapper = new ObjectMapper();
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
                "\"callback_url\":\"https://nonobediently-nonperishing-hilda.ngrok-free.dev/api/payments/webhook\"," +
                "\"return_url\":\"https://google.com\"," +
                "\"customization[title]\":\"TenaMed Payment\"," +
                "\"customization[description]\":\"Prescription Payment\"" +
                "}";

        String rawResponse = chapaHttpClient.initializeTransaction(jsonBody);
        System.out.println(txRef);
        return extractCheckoutUrl(rawResponse);
    }

    public String verifyPayment(String txRef) throws IOException {
        return chapaHttpClient.verifyTransaction(txRef);
    }

    public CancelPaymentResponse cancelPayment(String txRef) {
        if (txRef == null || txRef.isBlank()) {
            return new CancelPaymentResponse("tx_ref is required", "failed", null);
        }

        try {
            String rawResponse = chapaHttpClient.cancelTransaction(txRef);
            return mapCancelResponse(rawResponse);
        } catch (IOException ex) {
            return new CancelPaymentResponse("Unable to cancel transaction at this time", "failed", null);
        }
    }

    private CancelPaymentResponse mapCancelResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return new CancelPaymentResponse("Empty response from payment provider", "failed", null);
        }

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String message = root.path("message").asText("No message returned");
            String status = root.path("status").asText("failed");
            JsonNode dataNode = root.path("data");

            CancelPaymentData data = null;
            if (!dataNode.isMissingNode() && !dataNode.isNull()) {
                data = new CancelPaymentData(
                        dataNode.path("tx_ref").asText(null),
                        dataNode.path("amount").isNumber() ? dataNode.path("amount").asDouble() : null,
                        dataNode.path("currency").asText(null),
                        dataNode.path("created_at").asText(null),
                        dataNode.path("updated_at").asText(null)
                );
            }

            return new CancelPaymentResponse(message, status, data);
        } catch (IOException ignored) {
            return new CancelPaymentResponse("Invalid response from payment provider", "failed", null);
        }
    }

    private String extractCheckoutUrl(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String checkoutUrl = root.path("data").path("checkout_url").asText(null);
            if (checkoutUrl == null || checkoutUrl.isBlank()) {
                return null;
            }
            return checkoutUrl;
        } catch (IOException ignored) {
            return null;
        }
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
