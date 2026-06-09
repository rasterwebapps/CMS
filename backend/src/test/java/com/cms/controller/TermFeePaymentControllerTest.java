package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.TermFeePaymentDto;
import com.cms.dto.TermFeePaymentRequest;
import com.cms.model.enums.DemandStatus;
import com.cms.model.enums.PaymentMode;
import com.cms.service.TermFeePaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = TermFeePaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class TermFeePaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TermFeePaymentService termFeePaymentService;

    private TermFeePaymentDto buildPayment(Long id, String receipt) {
        return new TermFeePaymentDto(id, 1L, "Test Student",
            LocalDate.now(), BigDecimal.valueOf(5000), BigDecimal.ZERO,
            BigDecimal.valueOf(5000), PaymentMode.CASH, receipt,
            "remarks", DemandStatus.PARTIAL, Instant.now(), Instant.now());
    }

    @Test
    void shouldRecordPayment() throws Exception {
        TermFeePaymentRequest request = new TermFeePaymentRequest(
            1L, LocalDate.now(), BigDecimal.valueOf(5000), PaymentMode.CASH, "notes");

        TermFeePaymentDto response = buildPayment(1L, "RCP-001");
        when(termFeePaymentService.recordPayment(any(TermFeePaymentRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/term-fee-payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.receiptNumber").value("RCP-001"));
    }

    @Test
    void shouldReturnBadRequestWhenPaymentAmountIsZero() throws Exception {
        String invalidJson = """
            { "feeDemandId": 1, "paymentDate": "2025-01-01", "amountPaid": 0, "paymentMode": "CASH" }
            """;

        mockMvc.perform(post("/api/term-fee-payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenRequiredFieldsMissing() throws Exception {
        String invalidJson = """
            { "feeDemandId": 1 }
            """;

        mockMvc.perform(post("/api/term-fee-payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetPaymentsByDemandId() throws Exception {
        List<TermFeePaymentDto> payments = List.of(buildPayment(1L, "RCP-001"), buildPayment(2L, "RCP-002"));
        when(termFeePaymentService.getPaymentsByDemand(1L)).thenReturn(payments);

        mockMvc.perform(get("/api/term-fee-payments").param("demandId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldGetPaymentsByDateRange() throws Exception {
        List<TermFeePaymentDto> payments = List.of(buildPayment(1L, "RCP-001"));
        when(termFeePaymentService.getPaymentsByDateRange(
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))).thenReturn(payments);

        mockMvc.perform(get("/api/term-fee-payments")
                .param("from", "2025-01-01")
                .param("to", "2025-12-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldReturnBadRequestWhenNoParams() throws Exception {
        mockMvc.perform(get("/api/term-fee-payments"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetPaymentById() throws Exception {
        when(termFeePaymentService.getById(1L)).thenReturn(buildPayment(1L, "RCP-001"));

        mockMvc.perform(get("/api/term-fee-payments/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.receiptNumber").value("RCP-001"));
    }

    @Test
    void shouldGetPaymentByReceiptNumber() throws Exception {
        when(termFeePaymentService.getPaymentByReceipt("RCP-001")).thenReturn(buildPayment(1L, "RCP-001"));

        mockMvc.perform(get("/api/term-fee-payments/receipt/RCP-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.receiptNumber").value("RCP-001"));
    }
}

