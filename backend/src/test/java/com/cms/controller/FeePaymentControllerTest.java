package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.FeePaymentRequest;
import com.cms.dto.FeePaymentResponse;
import com.cms.dto.StudentFeeStatusResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.FeeType;
import com.cms.model.enums.PaymentMode;
import com.cms.model.enums.PaymentStatus;
import com.cms.service.FeePaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = FeePaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class FeePaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeePaymentService feePaymentService;

    @Test
    void shouldCreateFeePayment() throws Exception {
        FeePaymentRequest request = new FeePaymentRequest(
            1L, 1L, new BigDecimal("25000.00"), LocalDate.now(),
            PaymentMode.UPI, PaymentStatus.PARTIAL, "TXN123", null
        );

        FeePaymentResponse response = createResponse(1L, new BigDecimal("25000.00"), PaymentStatus.PARTIAL);

        when(feePaymentService.create(any(FeePaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/fee-payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("PARTIAL"));

        verify(feePaymentService).create(any(FeePaymentRequest.class));
    }

    @Test
    void shouldFindAllPayments() throws Exception {
        FeePaymentResponse response = createResponse(1L, new BigDecimal("25000.00"), PaymentStatus.PARTIAL);

        when(feePaymentService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/fee-payments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(feePaymentService).findAll();
    }

    @Test
    void shouldFindByStudentId() throws Exception {
        FeePaymentResponse response = createResponse(1L, new BigDecimal("25000.00"), PaymentStatus.PARTIAL);

        when(feePaymentService.findByStudentId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/fee-payments").param("studentId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(feePaymentService).findByStudentId(1L);
    }

    @Test
    void shouldFindByDateRange() throws Exception {
        FeePaymentResponse response = createResponse(1L, new BigDecimal("25000.00"), PaymentStatus.PARTIAL);

        when(feePaymentService.findByDateRange(any(), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/fee-payments")
                .param("startDate", LocalDate.now().minusDays(7).toString())
                .param("endDate", LocalDate.now().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(feePaymentService).findByDateRange(any(), any());
    }

    @Test
    void shouldFindById() throws Exception {
        FeePaymentResponse response = createResponse(1L, new BigDecimal("25000.00"), PaymentStatus.PARTIAL);

        when(feePaymentService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/fee-payments/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(feePaymentService).findById(1L);
    }

    @Test
    void shouldFindByReceiptNumber() throws Exception {
        FeePaymentResponse response = createResponse(1L, new BigDecimal("25000.00"), PaymentStatus.PARTIAL);

        when(feePaymentService.findByReceiptNumber("RCP-123")).thenReturn(response);

        mockMvc.perform(get("/fee-payments/receipt/RCP-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(feePaymentService).findByReceiptNumber("RCP-123");
    }

    @Test
    void shouldGetStudentFeeStatus() throws Exception {
        StudentFeeStatusResponse status = new StudentFeeStatusResponse(
            1L, "John Doe", "CS2024001",
            new BigDecimal("50000.00"), new BigDecimal("30000.00"), new BigDecimal("20000.00"),
            Collections.emptyList()
        );

        when(feePaymentService.getStudentFeeStatus(1L, 1L)).thenReturn(status);

        mockMvc.perform(get("/fee-payments/status")
                .param("studentId", "1")
                .param("academicYearId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalFees").value(50000.00))
            .andExpect(jsonPath("$.pendingAmount").value(20000.00));

        verify(feePaymentService).getStudentFeeStatus(1L, 1L);
    }

    @Test
    void shouldUpdateFeePayment() throws Exception {
        FeePaymentRequest request = new FeePaymentRequest(
            1L, 1L, new BigDecimal("50000.00"), LocalDate.now(),
            PaymentMode.CARD, PaymentStatus.PAID, "TXN456", "Full payment"
        );

        FeePaymentResponse response = createResponse(1L, new BigDecimal("50000.00"), PaymentStatus.PAID);

        when(feePaymentService.update(eq(1L), any(FeePaymentRequest.class))).thenReturn(response);

        mockMvc.perform(put("/fee-payments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"));

        verify(feePaymentService).update(eq(1L), any(FeePaymentRequest.class));
    }

    @Test
    void shouldDeleteFeePayment() throws Exception {
        doNothing().when(feePaymentService).delete(1L);

        mockMvc.perform(delete("/fee-payments/1"))
            .andExpect(status().isNoContent());

        verify(feePaymentService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeleting() throws Exception {
        doThrow(new ResourceNotFoundException("Fee payment not found with id: 999"))
            .when(feePaymentService).delete(999L);

        mockMvc.perform(delete("/fee-payments/999"))
            .andExpect(status().isNotFound());

        verify(feePaymentService).delete(999L);
    }

    private FeePaymentResponse createResponse(Long id, BigDecimal amount, PaymentStatus status) {
        Instant now = Instant.now();
        return new FeePaymentResponse(
            id, 1L, "John Doe", "CS2024001", 1L, FeeType.TUITION,
            new BigDecimal("50000.00"), "RCP-" + id, amount, LocalDate.now(),
            PaymentMode.UPI, status, "TXN123", null, now, now
        );
    }
}
