package com.cms.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.FeeCollectionSummaryDto;
import com.cms.dto.FeeDemandDto;
import com.cms.dto.StudentFeeLedgerDto;
import com.cms.dto.TermFeePaymentDto;
import com.cms.model.enums.DemandStatus;
import com.cms.model.enums.PaymentMode;
import com.cms.service.FeeReportService;

@WebMvcTest(controllers = FeeReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class FeeReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeeReportService feeReportService;

    @Test
    void shouldGetOutstandingDemands() throws Exception {
        FeeDemandDto demand = new FeeDemandDto(1L, 1L, 1L, "Student A", "CS-2024",
            1L, "Term 1", 1L, "2024-25",
            BigDecimal.valueOf(10000), LocalDate.now().plusDays(30),
            BigDecimal.ZERO, BigDecimal.valueOf(10000), DemandStatus.UNPAID);
        when(feeReportService.getOutstandingDemands(1L)).thenReturn(List.of(demand));

        mockMvc.perform(get("/api/fee-reports/outstanding").param("termInstanceId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("UNPAID"));
    }

    @Test
    void shouldGetCollectionSummary() throws Exception {
        FeeCollectionSummaryDto summary = new FeeCollectionSummaryDto(
            "Computer Science", "CS", 50L,
            BigDecimal.valueOf(500000), BigDecimal.valueOf(300000), BigDecimal.valueOf(200000),
            30L, 10L, 10L);
        when(feeReportService.getCollectionSummary(1L)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/fee-reports/collection-summary").param("termInstanceId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].programCode").value("CS"));
    }

    @Test
    void shouldGetLateFeeCollection() throws Exception {
        TermFeePaymentDto payment = new TermFeePaymentDto(1L, 1L, "Student A",
            LocalDate.now(), BigDecimal.valueOf(5000), BigDecimal.valueOf(200),
            BigDecimal.valueOf(5200), PaymentMode.CASH, "RCP-001",
            "Late payment", DemandStatus.PAID, Instant.now(), Instant.now());
        when(feeReportService.getLateFeeCollection(1L)).thenReturn(List.of(payment));

        mockMvc.perform(get("/api/fee-reports/late-fee-collection").param("termInstanceId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].receiptNumber").value("RCP-001"));
    }

    @Test
    void shouldGetStudentLedger() throws Exception {
        StudentFeeLedgerDto ledger = new StudentFeeLedgerDto(1L, "Student A", List.of());
        when(feeReportService.getStudentLedger(1L)).thenReturn(ledger);

        mockMvc.perform(get("/api/fee-reports/student-ledger").param("studentId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentId").value(1))
            .andExpect(jsonPath("$.studentName").value("Student A"));
    }
}

