package com.TenaMed.payment.service;

import com.TenaMed.payment.ChapaHttpClient;
import com.TenaMed.payment.dto.CancelPaymentData;
import com.TenaMed.payment.dto.CancelPaymentResponse;
import com.TenaMed.payment.dto.PaymentWebhookResponse;
import com.TenaMed.payment.entity.Payment;
import com.TenaMed.payment.repository.PaymentRepository;
import com.TenaMed.pharmacy.entity.Order;
import com.TenaMed.pharmacy.dto.response.OrderResponse;
import com.TenaMed.pharmacy.enums.PaymentStatus;
import com.TenaMed.pharmacy.repository.OrderRepository;
import com.TenaMed.pharmacy.service.OrderService;
import com.TenaMed.user.entity.User;
import com.TenaMed.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private final ChapaHttpClient chapaHttpClient;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public PaymentService(ChapaHttpClient chapaHttpClient,
                          PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          UserRepository userRepository,
                          OrderService orderService) {
        this.chapaHttpClient = chapaHttpClient;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.objectMapper = new ObjectMapper();
    }

    public String initializePayment(
            UUID orderId,
            UUID userId
    ) throws IOException {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!userId.equals(order.getCustomerId())) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        if (order.getTotalAmount() == null) {
            throw new IllegalArgumentException("Order amount is missing");
        }

        User user = userRepository.findWithAuthGraphById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getAccount() == null || user.getAccount().getEmail() == null || user.getAccount().getEmail().isBlank()) {
            throw new IllegalArgumentException("User email is required");
        }

        String amount = order.getTotalAmount().toPlainString();
        String email = user.getAccount().getEmail();

        UUID txRef = UUID.randomUUID();
        String txRefValue = txRef.toString();

        String jsonBody = "{" +
                "\"amount\":\"" + escapeJson(amount) + "\"," +
                "\"currency\":\"ETB\"," +
                "\"email\":\"" + escapeJson(email) + "\"," +
                "\"tx_ref\":\"" + txRefValue + "\"," +
                "\"callback_url\":\"https://nonobediently-nonperishing-hilda.ngrok-free.dev/api/payments/webhook\"," +
                "\"return_url\":\"https://google.com\"," +
                "\"customization[title]\":\"TenaMed Payment\"," +
                "\"customization[description]\":\"Prescription Payment\"" +
                "}";

        String rawResponse = chapaHttpClient.initializeTransaction(jsonBody);
        System.out.println(rawResponse);
        String checkoutUrl = extractCheckoutUrl(rawResponse);

        if (checkoutUrl != null && !checkoutUrl.isBlank()) {
            saveInitializedPayment(orderId, order.getTotalAmount(), txRef);
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

    @Transactional
    public PaymentWebhookResponse processWebhook(String txRefValue, boolean success) {
        UUID txRef = parseUuid(txRefValue);
        if (txRef == null) {
            return new PaymentWebhookResponse(
                    "Invalid tx_ref",
                    "failed",
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        Payment payment = paymentRepository.findByTxRef(txRef).orElse(null);
        if (payment == null) {
            return new PaymentWebhookResponse(
                    "Payment not found for tx_ref",
                    "failed",
                    txRef,
                    null,
                    null,
                    null,
                    null
            );
        }

        if ("SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            return new PaymentWebhookResponse(
                    "Payment already processed",
                    "success",
                    payment.getTxRef(),
                    payment.getId(),
                    payment.getOrderId(),
                    "CONFIRMED",
                    "SUCCESS"
            );
        }

        if (!success) {
            return new PaymentWebhookResponse(
                    "Payment verification failed",
                    "failed",
                    payment.getTxRef(),
                    payment.getId(),
                    payment.getOrderId(),
                    null,
                    null
            );
        }

        payment.setStatus("SUCCESS");
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        OrderResponse updatedOrder = orderService.updatePaymentStatus(payment.getOrderId(), PaymentStatus.SUCCESS);

        return new PaymentWebhookResponse(
                "Payment verified and order updated",
                "success",
                payment.getTxRef(),
                payment.getId(),
                payment.getOrderId(),
                updatedOrder.getStatus() == null ? null : updatedOrder.getStatus().name(),
                updatedOrder.getPaymentStatus() == null ? null : updatedOrder.getPaymentStatus().name()
        );
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

    private void saveInitializedPayment(UUID orderId, BigDecimal amount, UUID txRef) {
        Payment payment = paymentRepository.findByOrderId(orderId).orElseGet(Payment::new);
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setPaymentMethod("CHAPA");
        payment.setPaymentGateway("CHAPA");
        payment.setTxRef(txRef);
        payment.setStatus("PENDING");

        paymentRepository.save(payment);
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
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
