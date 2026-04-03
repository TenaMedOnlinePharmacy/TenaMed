package com.TenaMed.payment.service;

import com.TenaMed.payment.ChapaHttpClient;
import com.TenaMed.payment.dto.CancelPaymentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTests {

    @Mock
    private ChapaHttpClient chapaHttpClient;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldMapSuccessfulCancellationResponse() throws IOException {
        when(chapaHttpClient.cancelTransaction("tx-456-sdf")).thenReturn("""
                {
                  "message": "Checkout link expired successfully",
                  "status": "success",
                  "data": {
                      "tx_ref": "tx-456-sdf",
                      "amount": 5,
                      "currency": "ETB",
                      "created_at": "2025-10-22T09:10:03.000000Z",
                      "updated_at": "2025-10-22T09:10:21.000000Z"
                  }
                }
                """);

        CancelPaymentResponse response = paymentService.cancelPayment("tx-456-sdf");

        assertEquals("Checkout link expired successfully", response.getMessage());
        assertEquals("success", response.getStatus());
        assertNotNull(response.getData());
        assertEquals("tx-456-sdf", response.getData().getTxRef());
        assertEquals(5.0, response.getData().getAmount());
        assertEquals("ETB", response.getData().getCurrency());
    }

    @Test
    void shouldMapFailedCancellationResponseWithNullData() throws IOException {
        when(chapaHttpClient.cancelTransaction("tx-456-sdf")).thenReturn("""
                {
                  "message": "Payment link already expired",
                  "status": "failed",
                  "data": null
                }
                """);

        CancelPaymentResponse response = paymentService.cancelPayment("tx-456-sdf");

        assertEquals("Payment link already expired", response.getMessage());
        assertEquals("failed", response.getStatus());
        assertNull(response.getData());
    }

    @Test
    void shouldReturnFailedWhenTxRefIsBlank() {
        CancelPaymentResponse response = paymentService.cancelPayment("  ");

        assertEquals("tx_ref is required", response.getMessage());
        assertEquals("failed", response.getStatus());
        assertNull(response.getData());
        verifyNoInteractions(chapaHttpClient);
    }

    @Test
    void shouldReturnFailedWhenClientThrowsIOException() throws IOException {
        when(chapaHttpClient.cancelTransaction("tx-500")).thenThrow(new IOException("network error"));

        CancelPaymentResponse response = paymentService.cancelPayment("tx-500");

        assertEquals("Unable to cancel transaction at this time", response.getMessage());
        assertEquals("failed", response.getStatus());
        assertNull(response.getData());
    }
}
