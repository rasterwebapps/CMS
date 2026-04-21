package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.cms.dto.EnquiryConversionRequest;
import com.cms.dto.EnquiryPaymentRequest;
import com.cms.dto.EnquiryPaymentResponse;
import com.cms.dto.EnquiryRequest;
import com.cms.dto.EnquiryResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.enums.EnquiryStatus;
import com.cms.model.enums.PaymentMode;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.service.EnquiryDocumentService;
import com.cms.service.EnquiryPaymentService;
import com.cms.service.EnquiryService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = EnquiryController.class)
@AutoConfigureMockMvc(addFilters = false)
class EnquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EnquiryService enquiryService;

    @MockitoBean
    private EnquiryDocumentService enquiryDocumentService;

    @MockitoBean
    private EnquiryPaymentRepository enquiryPaymentRepository;

    @MockitoBean
    private EnquiryPaymentService enquiryPaymentService;

    @Test
    void shouldCreateEnquiry() throws Exception {
        EnquiryRequest request = new EnquiryRequest(
            "Ravi Kumar", "ravi@email.com", "9876543210", 1L, null,
            LocalDate.of(2024, 6, 15), 1L, EnquiryStatus.ENQUIRED,
            null, "Interested in CS", new BigDecimal("50000.00"),
            null, null, null, null, null
        );

        EnquiryResponse response = createResponse(1L, "Ravi Kumar", EnquiryStatus.ENQUIRED);

        when(enquiryService.create(any(EnquiryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/enquiries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Ravi Kumar"));

        verify(enquiryService).create(any(EnquiryRequest.class));
    }

    @Test
    void shouldFindAllEnquiries() throws Exception {
        EnquiryResponse response = createResponse(1L, "Ravi Kumar", EnquiryStatus.ENQUIRED);

        when(enquiryService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/enquiries"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(enquiryService).findAll();
    }

    @Test
    void shouldFindByStatus() throws Exception {
        EnquiryResponse response = createResponse(1L, "Ravi Kumar", EnquiryStatus.ENQUIRED);

        when(enquiryService.findByStatus(EnquiryStatus.ENQUIRED)).thenReturn(List.of(response));

        mockMvc.perform(get("/enquiries").param("status", "ENQUIRED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(enquiryService).findByStatus(EnquiryStatus.ENQUIRED);
    }

    @Test
    void shouldFindByReferralTypeId() throws Exception {
        EnquiryResponse response = createResponse(1L, "Ravi Kumar", EnquiryStatus.ENQUIRED);

        when(enquiryService.findByReferralTypeId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/enquiries").param("referralTypeId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(enquiryService).findByReferralTypeId(1L);
    }

    @Test
    void shouldFindById() throws Exception {
        EnquiryResponse response = createResponse(1L, "Ravi Kumar", EnquiryStatus.ENQUIRED);

        when(enquiryService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/enquiries/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Ravi Kumar"));

        verify(enquiryService).findById(1L);
    }

    @Test
    void shouldReturnNotFound() throws Exception {
        when(enquiryService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Enquiry not found with id: 999"));

        mockMvc.perform(get("/enquiries/999"))
            .andExpect(status().isNotFound());

        verify(enquiryService).findById(999L);
    }

    @Test
    void shouldUpdateEnquiry() throws Exception {
        EnquiryRequest request = new EnquiryRequest(
            "Ravi Kumar Updated", "ravi@email.com", "9876543210", 1L, null,
            LocalDate.of(2024, 6, 20), 1L, EnquiryStatus.INTERESTED,
            null, "Called back", new BigDecimal("45000.00"),
            null, null, null, null, null
        );

        EnquiryResponse response = createResponse(1L, "Ravi Kumar Updated", EnquiryStatus.INTERESTED);

        when(enquiryService.update(eq(1L), any(EnquiryRequest.class))).thenReturn(response);

        mockMvc.perform(put("/enquiries/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Ravi Kumar Updated"));

        verify(enquiryService).update(eq(1L), any(EnquiryRequest.class));
    }

    @Test
    void shouldConvertToStudent() throws Exception {
        EnquiryResponse response = new EnquiryResponse(
            1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            1L, "B.Tech CS", null, null, LocalDate.of(2024, 6, 15),
            1L, "Walk In", null, false,
            EnquiryStatus.CONVERTED,
            null, null, "Converted", new BigDecimal("50000.00"),
            null, null, null, null, null,
            null, null, null, null, null, null,
            10L, Instant.now(), Instant.now()
        );

        when(enquiryService.convertToStudent(1L, 10L)).thenReturn(response);

        mockMvc.perform(put("/enquiries/1/convert").param("studentId", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONVERTED"))
            .andExpect(jsonPath("$.convertedStudentId").value(10));

        verify(enquiryService).convertToStudent(1L, 10L);
    }

    @Test
    void shouldConvertToStudentWithData() throws Exception {
        EnquiryConversionRequest request = new EnquiryConversionRequest(
            "Ravi", "Kumar", "ravi@college.edu", "9876543210", 1,
            LocalDate.of(2024, 7, 1), 2024, 2025, LocalDate.of(2024, 7, 1), true, true
        );

        EnquiryResponse response = new EnquiryResponse(
            1L, "Ravi Kumar", "ravi@email.com", "9876543210",
            1L, "B.Tech CS", null, null, LocalDate.of(2024, 6, 15),
            1L, "Walk In", null, false,
            EnquiryStatus.ADMITTED,
            null, null, "Admitted", new BigDecimal("50000.00"),
            null, null, null, null, null,
            null, null, null, null, null, null,
            10L, Instant.now(), Instant.now()
        );

        when(enquiryService.convertToStudentWithData(eq(1L), any(EnquiryConversionRequest.class), any()))
            .thenReturn(response);

        mockMvc.perform(post("/enquiries/1/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ADMITTED"))
            .andExpect(jsonPath("$.convertedStudentId").value(10));

        verify(enquiryService).convertToStudentWithData(eq(1L), any(EnquiryConversionRequest.class), any());
    }

    @Test
    void shouldDeleteEnquiry() throws Exception {
        doNothing().when(enquiryService).delete(1L);

        mockMvc.perform(delete("/enquiries/1"))
            .andExpect(status().isNoContent());

        verify(enquiryService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeleting() throws Exception {
        doThrow(new ResourceNotFoundException("Enquiry not found with id: 999"))
            .when(enquiryService).delete(999L);

        mockMvc.perform(delete("/enquiries/999"))
            .andExpect(status().isNotFound());

        verify(enquiryService).delete(999L);
    }

    @Test
    void shouldFindByDateRange() throws Exception {
        EnquiryResponse response = createResponse(1L, "Ravi Kumar", EnquiryStatus.ENQUIRED);

        when(enquiryService.findByDateRange(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30)))
            .thenReturn(List.of(response));

        mockMvc.perform(get("/enquiries")
                .param("fromDate", "2024-06-01")
                .param("toDate", "2024-06-30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(enquiryService).findByDateRange(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30));
    }

    @Test
    void shouldFindByDateRangeAndStatus() throws Exception {
        EnquiryResponse response = createResponse(1L, "Ravi Kumar", EnquiryStatus.ENQUIRED);

        when(enquiryService.findByDateRangeAndStatus(
            LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30), EnquiryStatus.ENQUIRED))
            .thenReturn(List.of(response));

        mockMvc.perform(get("/enquiries")
                .param("fromDate", "2024-06-01")
                .param("toDate", "2024-06-30")
                .param("status", "ENQUIRED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(enquiryService).findByDateRangeAndStatus(
            LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30), EnquiryStatus.ENQUIRED);
    }

    @Test
    void shouldUpdateStatus() throws Exception {
        EnquiryResponse response = createResponse(1L, "Ravi Kumar", EnquiryStatus.INTERESTED);

        when(enquiryService.updateStatus(eq(1L), eq(EnquiryStatus.INTERESTED), any(String.class))).thenReturn(response);

        mockMvc.perform(patch("/enquiries/1/status").param("status", "INTERESTED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INTERESTED"));

        verify(enquiryService).updateStatus(eq(1L), eq(EnquiryStatus.INTERESTED), any(String.class));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingStatusForNonExistent() throws Exception {
        when(enquiryService.updateStatus(eq(999L), any(EnquiryStatus.class), any(String.class)))
            .thenThrow(new ResourceNotFoundException("Enquiry not found with id: 999"));

        mockMvc.perform(patch("/enquiries/999/status").param("status", "INTERESTED"))
            .andExpect(status().isNotFound());

        verify(enquiryService).updateStatus(eq(999L), eq(EnquiryStatus.INTERESTED), any(String.class));
    }

    @Test
    void shouldFinalizeFees() throws Exception {
        com.cms.dto.FeeFinalizationRequest request = new com.cms.dto.FeeFinalizationRequest(
            new BigDecimal("100000.00"), new BigDecimal("5000.00"), "Early bird", null
        );

        com.cms.dto.FeeFinalizationResponse response = new com.cms.dto.FeeFinalizationResponse(
            1L, new BigDecimal("100000.00"), new BigDecimal("5000.00"), "Early bird",
            new BigDecimal("95000.00"), "admin", Instant.now(), "FEES_FINALIZED"
        );

        when(enquiryService.finalizeFees(eq(1L), any(com.cms.dto.FeeFinalizationRequest.class), any(String.class)))
            .thenReturn(response);

        mockMvc.perform(post("/enquiries/1/finalize-fees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enquiryId").value(1))
            .andExpect(jsonPath("$.status").value("FEES_FINALIZED"));

        verify(enquiryService).finalizeFees(eq(1L), any(com.cms.dto.FeeFinalizationRequest.class), any(String.class));
    }

    @Test
    void shouldSubmitDocuments() throws Exception {
        EnquiryResponse response = createResponse(1L, "Ravi Kumar", EnquiryStatus.DOCUMENTS_SUBMITTED);

        when(enquiryDocumentService.allMandatoryDocumentsSubmitted(1L))
            .thenReturn(new com.cms.dto.MissingDocumentsResponse(true, List.of()));
        when(enquiryService.submitDocuments(1L)).thenReturn(response);

        mockMvc.perform(post("/enquiries/1/submit-documents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DOCUMENTS_SUBMITTED"));

        verify(enquiryService).submitDocuments(1L);
    }

    @Test
    void shouldReturnBadRequestWhenMandatoryDocumentsMissing() throws Exception {
        when(enquiryDocumentService.allMandatoryDocumentsSubmitted(1L))
            .thenReturn(new com.cms.dto.MissingDocumentsResponse(false, List.of("IDENTITY_PROOF")));

        mockMvc.perform(post("/enquiries/1/submit-documents"))
            .andExpect(status().isBadRequest());

        verify(enquiryService, never()).submitDocuments(any());
    }

    @Test
    void shouldCollectPayment() throws Exception {
        EnquiryPaymentRequest request = new EnquiryPaymentRequest(
            new java.math.BigDecimal("25000.00"),
            LocalDate.of(2024, 7, 1),
            PaymentMode.CASH,
            null,
            "First instalment"
        );

        EnquiryPaymentResponse response = new EnquiryPaymentResponse(
            1L, 1L, "Ravi Kumar",
            new java.math.BigDecimal("25000.00"),
            LocalDate.of(2024, 7, 1),
            PaymentMode.CASH,
            null,
            "First instalment",
            "RCP-20240701-ABCD1234",
            "system",
            "PARTIALLY_PAID",
            Instant.now()
        );

        when(enquiryPaymentService.collectPayment(eq(1L), any(EnquiryPaymentRequest.class), any(String.class)))
            .thenReturn(response);

        mockMvc.perform(post("/enquiries/1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.receiptNumber").value("RCP-20240701-ABCD1234"))
            .andExpect(jsonPath("$.newStatus").value("PARTIALLY_PAID"));

        verify(enquiryPaymentService).collectPayment(eq(1L), any(EnquiryPaymentRequest.class), any(String.class));
    }

    @Test
    void shouldGetPayments() throws Exception {
        EnquiryPaymentResponse response = new EnquiryPaymentResponse(
            1L, 1L, "Ravi Kumar",
            new java.math.BigDecimal("25000.00"),
            LocalDate.of(2024, 7, 1),
            PaymentMode.UPI,
            "TXN123",
            null,
            "RCP-20240701-ABCD1234",
            "system",
            null,
            Instant.now()
        );

        when(enquiryPaymentService.getPaymentsByEnquiryId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/enquiries/1/payments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].receiptNumber").value("RCP-20240701-ABCD1234"));

        verify(enquiryPaymentService).getPaymentsByEnquiryId(1L);
    }

    private EnquiryResponse createResponse(Long id, String name, EnquiryStatus status) {
        Instant now = Instant.now();
        return new EnquiryResponse(
            id, name, "ravi@email.com", "9876543210",
            1L, "B.Tech CS", null, null, LocalDate.of(2024, 6, 15),
            1L, "Walk In", null, false,
            status, null, null, "Remarks",
            new BigDecimal("50000.00"), null, null, null, null, null,
            null, null, null, null, null, null,
            null, now, now
        );
    }
}
