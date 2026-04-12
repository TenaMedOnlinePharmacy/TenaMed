package com.TenaMed.payment.service;

import com.TenaMed.payment.ChapaHttpClient;
import com.TenaMed.payment.dto.CancelPaymentData;
import com.TenaMed.payment.dto.CancelPaymentResponse;
import com.TenaMed.payment.entity.Payment;
import com.TenaMed.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    private final ChapaHttpClient chapaHttpClient;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    public PaymentService(ChapaHttpClient chapaHttpClient, PaymentRepository paymentRepository) {
        this.chapaHttpClient = chapaHttpClient;
        this.paymentRepository = paymentRepository;
        this.objectMapper = new ObjectMapper();
    }

    public String initializePayment(
            UUID orderId,
            String amount,
            String paymentMethod,
            String email,
            String firstName,
            String lastName,
            String phoneNumber
    ) throws IOException {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId is required");
        }

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
        String checkoutUrl = extractCheckoutUrl(rawResponse);

        if (checkoutUrl != null && !checkoutUrl.isBlank()) {
            saveInitializedPayment(orderId, amount, paymentMethod, txRef);
        }

        return checkoutUrl;
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

    private void saveInitializedPayment(UUID orderId, String amount, String paymentMethod, String txRef) {
        Payment payment = paymentRepository.findByOrderId(orderId).orElseGet(Payment::new);
        payment.setOrderId(orderId);
        payment.setAmount(parseAmount(amount));
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentGateway("CHAPA");
        payment.setGatewayTransactionId(txRef);
        payment.setStatus("PENDING");

        paymentRepository.save(payment);
    }

    private BigDecimal parseAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(amount.trim());
        } catch (NumberFormatException ex) {
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
