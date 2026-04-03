package com.TenaMed.payment.controller;

import com.TenaMed.payment.dto.CancelPaymentData;
import com.TenaMed.payment.dto.CancelPaymentResponse;
import com.TenaMed.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void shouldDelegateCancellationToServiceAndReturnMappedResponse() throws Exception {
        CancelPaymentResponse response = new CancelPaymentResponse(
                "Checkout link expired successfully",
                "success",
                new CancelPaymentData(
                        "tx-456-sdf",
                        5.0,
                        "ETB",
                        "2025-10-22T09:10:03.000000Z",
                        "2025-10-22T09:10:21.000000Z"
                )
        );

        when(paymentService.cancelPayment("tx-456-sdf")).thenReturn(response);

        mockMvc.perform(put("/api/payments/cancel/tx-456-sdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Checkout link expired successfully"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.tx_ref").value("tx-456-sdf"))
                .andExpect(jsonPath("$.data.amount").value(5.0))
                .andExpect(jsonPath("$.data.currency").value("ETB"));
    }
}
