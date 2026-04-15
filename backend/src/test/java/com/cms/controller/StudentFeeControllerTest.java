package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.cms.dto.CollectPaymentRequest;
import com.cms.dto.CollectPaymentResponse;
import com.cms.dto.FeeExplorerResponse;
import com.cms.dto.PenaltyResponse;
import com.cms.dto.ReceiptResponse;
import com.cms.dto.StudentFeeAllocationRequest;
import com.cms.dto.StudentFeeAllocationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.PaymentMode;
import com.cms.service.FeeExplorerService;
import com.cms.service.FeeFinalizationService;
import com.cms.service.PaymentCollectionService;
import com.cms.service.PenaltyCalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = StudentFeeController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentFeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeeFinalizationService feeFinalizationService;

    @MockitoBean
    private PaymentCollectionService paymentCollectionService;

    @MockitoBean
    private PenaltyCalculationService penaltyCalculationService;

    @MockitoBean
    private FeeExplorerService feeExplorerService;

    @Test
    void shouldFinalizeFeeAllocation() throws Exception {
        StudentFeeAllocationRequest request = new StudentFeeAllocationRequest(
            1L,
            new BigDecimal("200000.00"),
            new BigDecimal("10000.00"),
            "Merit scholarship",
            new BigDecimal("5000.00"),
            List.of(
                new StudentFeeAllocationRequest.YearFee(1, new BigDecimal("100000.00"), LocalDate.of(2025, 6, 1)),
                new StudentFeeAllocationRequest.YearFee(2, new BigDecimal("100000.00"), LocalDate.of(2026, 6, 1))
            )
        );

        StudentFeeAllocationResponse response = createAllocationResponse();

        when(feeFinalizationService.finalize(any(StudentFeeAllocationRequest.class), any(String.class)))
            .thenReturn(response);

        mockMvc.perform(post("/student-fees/finalize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.studentId").value(1))
            .andExpect(jsonPath("$.studentName").value("John Doe"))
            .andExpect(jsonPath("$.rollNumber").value("CS2024001"))
            .andExpect(jsonPath("$.totalFee").value(200000.00))
            .andExpect(jsonPath("$.netFee").value(185000.00))
            .andExpect(jsonPath("$.status").value("FINALIZED"))
            .andExpect(jsonPath("$.semesterFees.length()").value(2));

        verify(feeFinalizationService).finalize(any(StudentFeeAllocationRequest.class), any(String.class));
    }

    @Test
    void shouldGetSemesterBreakdown() throws Exception {
        StudentFeeAllocationResponse response = createAllocationResponse();

        when(feeFinalizationService.getByStudentId(1L)).thenReturn(response);

        mockMvc.perform(get("/student-fees/1/semester-breakdown"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.studentId").value(1))
            .andExpect(jsonPath("$.studentName").value("John Doe"))
            .andExpect(jsonPath("$.semesterFees.length()").value(2))
            .andExpect(jsonPath("$.semesterFees[0].yearNumber").value(1))
            .andExpect(jsonPath("$.semesterFees[0].semesterLabel").value("Year 1"))
            .andExpect(jsonPath("$.semesterFees[0].amount").value(100000.00))
            .andExpect(jsonPath("$.semesterFees[1].yearNumber").value(2));

        verify(feeFinalizationService).getByStudentId(1L);
    }

    @Test
    void shouldReturnNotFoundForSemesterBreakdown() throws Exception {
        when(feeFinalizationService.getByStudentId(999L))
            .thenThrow(new ResourceNotFoundException("Student fee allocation not found for student id: 999"));

        mockMvc.perform(get("/student-fees/999/semester-breakdown"))
            .andExpect(status().isNotFound());

        verify(feeFinalizationService).getByStudentId(999L);
    }

    @Test
    void shouldCollectPayment() throws Exception {
        CollectPaymentRequest request = new CollectPaymentRequest(
            new BigDecimal("50000.00"),
            LocalDate.of(2025, 1, 15),
            PaymentMode.UPI,
            "TXN-UPI-12345",
            "First installment"
        );

        CollectPaymentResponse response = createCollectPaymentResponse();

        when(paymentCollectionService.collectPayment(eq(1L), any(CollectPaymentRequest.class)))
            .thenReturn(response);

        mockMvc.perform(post("/student-fees/1/collect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.receiptNumber").value("RCP-2025-0001"))
            .andExpect(jsonPath("$.studentId").value(1))
            .andExpect(jsonPath("$.studentName").value("John Doe"))
            .andExpect(jsonPath("$.rollNumber").value("CS2024001"))
            .andExpect(jsonPath("$.amountPaid").value(50000.00))
            .andExpect(jsonPath("$.paymentMode").value("UPI"))
            .andExpect(jsonPath("$.transactionReference").value("TXN-UPI-12345"));

        verify(paymentCollectionService).collectPayment(eq(1L), any(CollectPaymentRequest.class));
    }

    @Test
    void shouldReturnNotFoundWhenCollectingPaymentForUnknownStudent() throws Exception {
        CollectPaymentRequest request = new CollectPaymentRequest(
            new BigDecimal("50000.00"),
            LocalDate.of(2025, 1, 15),
            PaymentMode.CASH,
            null,
            null
        );

        when(paymentCollectionService.collectPayment(eq(999L), any(CollectPaymentRequest.class)))
            .thenThrow(new ResourceNotFoundException("Student not found with id: 999"));

        mockMvc.perform(post("/student-fees/999/collect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        verify(paymentCollectionService).collectPayment(eq(999L), any(CollectPaymentRequest.class));
    }

    @Test
    void shouldGetPenalties() throws Exception {
        PenaltyResponse response = createPenaltyResponse();

        when(penaltyCalculationService.calculatePenalties(1L)).thenReturn(response);

        mockMvc.perform(get("/student-fees/1/penalties"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentId").value(1))
            .andExpect(jsonPath("$.studentName").value("John Doe"))
            .andExpect(jsonPath("$.rollNumber").value("CS2024001"))
            .andExpect(jsonPath("$.totalPenalty").value(1500.00))
            .andExpect(jsonPath("$.penalties.length()").value(1))
            .andExpect(jsonPath("$.penalties[0].semesterLabel").value("Year 1"))
            .andExpect(jsonPath("$.penalties[0].overdueDays").value(30))
            .andExpect(jsonPath("$.penalties[0].isPaid").value(false));

        verify(penaltyCalculationService).calculatePenalties(1L);
    }

    @Test
    void shouldReturnNotFoundForPenalties() throws Exception {
        when(penaltyCalculationService.calculatePenalties(999L))
            .thenThrow(new ResourceNotFoundException("Student fee allocation not found for student id: 999"));

        mockMvc.perform(get("/student-fees/999/penalties"))
            .andExpect(status().isNotFound());

        verify(penaltyCalculationService).calculatePenalties(999L);
    }

    @Test
    void shouldSearchFeeExplorerWithSearchParam() throws Exception {
        FeeExplorerResponse response = createFeeExplorerResponse();

        when(feeExplorerService.search("John")).thenReturn(response);

        mockMvc.perform(get("/student-fees/explorer").param("search", "John"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.students.length()").value(1))
            .andExpect(jsonPath("$.students[0].studentId").value(1))
            .andExpect(jsonPath("$.students[0].studentName").value("John Doe"))
            .andExpect(jsonPath("$.students[0].rollNumber").value("CS2024001"))
            .andExpect(jsonPath("$.students[0].programName").value("B.Tech Computer Science"))
            .andExpect(jsonPath("$.students[0].totalFee").value(200000.00))
            .andExpect(jsonPath("$.students[0].totalPaid").value(50000.00))
            .andExpect(jsonPath("$.students[0].totalPending").value(150000.00))
            .andExpect(jsonPath("$.students[0].allocationStatus").value("FINALIZED"));

        verify(feeExplorerService).search("John");
    }

    @Test
    void shouldSearchFeeExplorerWithoutSearchParam() throws Exception {
        FeeExplorerResponse response = createFeeExplorerResponse();

        when(feeExplorerService.search(isNull())).thenReturn(response);

        mockMvc.perform(get("/student-fees/explorer"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.students.length()").value(1))
            .andExpect(jsonPath("$.students[0].studentId").value(1));

        verify(feeExplorerService).search(isNull());
    }

    @Test
    void shouldSearchFeeExplorerWithEmptyResults() throws Exception {
        FeeExplorerResponse emptyResponse = new FeeExplorerResponse(Collections.emptyList());

        when(feeExplorerService.search("nonexistent")).thenReturn(emptyResponse);

        mockMvc.perform(get("/student-fees/explorer").param("search", "nonexistent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.students.length()").value(0));

        verify(feeExplorerService).search("nonexistent");
    }

    @Test
    void shouldGetReceipts() throws Exception {
        List<ReceiptResponse> receipts = List.of(
            createReceiptResponse(1L, "RCP-2025-0001"),
            createReceiptResponse(2L, "RCP-2025-0002")
        );

        when(paymentCollectionService.getReceipts(1L)).thenReturn(receipts);

        mockMvc.perform(get("/student-fees/1/receipts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].receiptNumber").value("RCP-2025-0001"))
            .andExpect(jsonPath("$[0].studentId").value(1))
            .andExpect(jsonPath("$[0].studentName").value("John Doe"))
            .andExpect(jsonPath("$[0].amountPaid").value(50000.00))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].receiptNumber").value("RCP-2025-0002"));

        verify(paymentCollectionService).getReceipts(1L);
    }

    @Test
    void shouldGetEmptyReceiptsList() throws Exception {
        when(paymentCollectionService.getReceipts(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/student-fees/1/receipts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        verify(paymentCollectionService).getReceipts(1L);
    }

    @Test
    void shouldReturnNotFoundForReceipts() throws Exception {
        when(paymentCollectionService.getReceipts(999L))
            .thenThrow(new ResourceNotFoundException("Student not found with id: 999"));

        mockMvc.perform(get("/student-fees/999/receipts"))
            .andExpect(status().isNotFound());

        verify(paymentCollectionService).getReceipts(999L);
    }

    @Test
    void shouldGetReceiptById() throws Exception {
        ReceiptResponse receipt = createReceiptResponse(1L, "RCP-2025-0001");

        when(paymentCollectionService.getReceiptById(1L, 1L)).thenReturn(receipt);

        mockMvc.perform(get("/student-fees/1/receipts/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.receiptNumber").value("RCP-2025-0001"))
            .andExpect(jsonPath("$.studentId").value(1))
            .andExpect(jsonPath("$.studentName").value("John Doe"))
            .andExpect(jsonPath("$.rollNumber").value("CS2024001"))
            .andExpect(jsonPath("$.semesterFeeId").value(10))
            .andExpect(jsonPath("$.semesterLabel").value("Year 1"))
            .andExpect(jsonPath("$.yearNumber").value(1))
            .andExpect(jsonPath("$.amountPaid").value(50000.00))
            .andExpect(jsonPath("$.paymentMode").value("UPI"))
            .andExpect(jsonPath("$.transactionReference").value("TXN-UPI-12345"));

        verify(paymentCollectionService).getReceiptById(1L, 1L);
    }

    @Test
    void shouldReturnNotFoundForReceiptById() throws Exception {
        when(paymentCollectionService.getReceiptById(1L, 999L))
            .thenThrow(new ResourceNotFoundException("Receipt not found with id: 999"));

        mockMvc.perform(get("/student-fees/1/receipts/999"))
            .andExpect(status().isNotFound());

        verify(paymentCollectionService).getReceiptById(1L, 999L);
    }

    private StudentFeeAllocationResponse createAllocationResponse() {
        Instant now = Instant.now();
        return new StudentFeeAllocationResponse(
            1L, 1L, "John Doe", "CS2024001", 1L, "B.Tech Computer Science",
            new BigDecimal("200000.00"), new BigDecimal("10000.00"), "Merit scholarship",
            new BigDecimal("5000.00"), new BigDecimal("185000.00"), "FINALIZED",
            now, "admin",
            List.of(
                new StudentFeeAllocationResponse.SemesterFeeDetail(
                    10L, 1, "Year 1", new BigDecimal("100000.00"),
                    LocalDate.of(2025, 6, 1), new BigDecimal("50000.00"),
                    new BigDecimal("50000.00"), BigDecimal.ZERO, "PARTIAL"
                ),
                new StudentFeeAllocationResponse.SemesterFeeDetail(
                    11L, 2, "Year 2", new BigDecimal("100000.00"),
                    LocalDate.of(2026, 6, 1), BigDecimal.ZERO,
                    new BigDecimal("100000.00"), BigDecimal.ZERO, "UNPAID"
                )
            ),
            now, now
        );
    }

    private CollectPaymentResponse createCollectPaymentResponse() {
        return new CollectPaymentResponse(
            "RCP-2025-0001", 1L, "John Doe", "CS2024001",
            new BigDecimal("50000.00"), LocalDate.of(2025, 1, 15),
            PaymentMode.UPI, "TXN-UPI-12345", "First installment",
            "Year 1: 50000.00 paid", Instant.now()
        );
    }

    private PenaltyResponse createPenaltyResponse() {
        return new PenaltyResponse(
            1L, "John Doe", "CS2024001",
            new BigDecimal("1500.00"),
            List.of(
                new PenaltyResponse.PenaltyDetail(
                    1L, 10L, "Year 1", 1,
                    new BigDecimal("50.00"),
                    LocalDate.of(2025, 6, 1), LocalDate.of(2025, 7, 1),
                    30L, new BigDecimal("1500.00"), false
                )
            )
        );
    }

    private FeeExplorerResponse createFeeExplorerResponse() {
        return new FeeExplorerResponse(
            List.of(
                new FeeExplorerResponse.StudentFeeSummary(
                    1L, "John Doe", "CS2024001", "B.Tech Computer Science",
                    4, new BigDecimal("200000.00"), new BigDecimal("50000.00"),
                    new BigDecimal("150000.00"), new BigDecimal("1500.00"),
                    "FINALIZED"
                )
            )
        );
    }

    private ReceiptResponse createReceiptResponse(Long id, String receiptNumber) {
        return new ReceiptResponse(
            id, receiptNumber, 1L, "John Doe", "CS2024001",
            10L, "Year 1", 1,
            new BigDecimal("50000.00"), LocalDate.of(2025, 1, 15),
            PaymentMode.UPI, "TXN-UPI-12345", "Installment payment",
            Instant.now()
        );
    }
}
