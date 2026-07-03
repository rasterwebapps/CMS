package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.OneBookPaymentSummaryResponse;
import com.cms.model.OneBookPaymentRequest;
import com.cms.repository.OneBookPaymentRequestRepository;
import com.cms.service.OneBookIntegrationService;
import com.cms.service.ScholarshipDisbursementService;
import com.cms.service.StudentScholarshipService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = ScholarshipApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ScholarshipApplicationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private StudentScholarshipService studentScholarshipService;
    @MockitoBean private ScholarshipDisbursementService disbursementService;
    @MockitoBean private OneBookIntegrationService oneBookService;
    @MockitoBean private OneBookPaymentRequestRepository obRepo;

    // ── GET /{id}/onebook-payments ────────────────────────────────────────────

    @Test
    void oneBookPayments_returnsListOfPaymentSummaries() throws Exception {
        OneBookPaymentRequest req1 = obRequest("DSB-2026-001", "TRANSMITTED");
        OneBookPaymentRequest req2 = obRequest("DSB-2026-002", "FAILED");
        req2.setErrorMessage("OneBook API call failed: Connection refused");

        when(obRepo.findByEntityIdAndPaymentTypeOrderByCreatedAtDesc(5L, "SCHOLARSHIP"))
                .thenReturn(List.of(req1, req2));

        mockMvc.perform(get("/scholarship-applications/5/onebook-payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].invoiceNumber").value("DSB-2026-001"))
                .andExpect(jsonPath("$[0].status").value("TRANSMITTED"))
                .andExpect(jsonPath("$[1].invoiceNumber").value("DSB-2026-002"))
                .andExpect(jsonPath("$[1].status").value("FAILED"))
                .andExpect(jsonPath("$[1].errorMessage").value("OneBook API call failed: Connection refused"));

        verify(obRepo).findByEntityIdAndPaymentTypeOrderByCreatedAtDesc(5L, "SCHOLARSHIP");
    }

    @Test
    void oneBookPayments_returnsEmptyList_whenNoPaymentsFound() throws Exception {
        when(obRepo.findByEntityIdAndPaymentTypeOrderByCreatedAtDesc(99L, "SCHOLARSHIP"))
                .thenReturn(List.of());

        mockMvc.perform(get("/scholarship-applications/99/onebook-payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── POST /{id}/disburse-onebook ───────────────────────────────────────────

    @Test
    void disburseViaOneBook_returnsReferenceIdAndStatus_onSuccess() throws Exception {
        OneBookPaymentRequest obReq = obRequest("DSB-2026-001", "TRANSMITTED");

        when(oneBookService.pushScholarshipPayment(eq(10L), any(), any())).thenReturn(obReq);

        String body = """
                {
                  "amount": 5000,
                  "disbursementDate": "2026-07-03",
                  "disbursementMode": "DIRECT_CREDIT"
                }
                """;

        mockMvc.perform(post("/scholarship-applications/10/disburse-onebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceId").value("OB-REF-001"))
                .andExpect(jsonPath("$.status").value("TRANSMITTED"));

        verify(oneBookService).pushScholarshipPayment(eq(10L), any(), any());
    }

    @Test
    void disburseViaOneBook_returnsFailedStatus_whenTransmissionFails() throws Exception {
        OneBookPaymentRequest obReq = obRequest("DSB-2026-001", "FAILED");
        obReq.setErrorMessage("OneBook API call failed: timeout");

        when(oneBookService.pushScholarshipPayment(eq(10L), any(), any())).thenReturn(obReq);

        String body = """
                {
                  "amount": 5000,
                  "disbursementDate": "2026-07-03",
                  "disbursementMode": "DIRECT_CREDIT"
                }
                """;

        mockMvc.perform(post("/scholarship-applications/10/disburse-onebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private OneBookPaymentRequest obRequest(String invoiceNumber, String status) {
        OneBookPaymentRequest r = new OneBookPaymentRequest();
        r.setReferenceId("OB-REF-001");
        r.setInvoiceNumber(invoiceNumber);
        r.setPaymentType("SCHOLARSHIP");
        r.setEntityId(5L);
        r.setAmount(new BigDecimal("5000"));
        r.setStatus(status);
        r.setTransmittedAt(status.equals("TRANSMITTED") ? Instant.now() : null);
        return r;
    }
}
