package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.OneBookPostingTrackCompletionPayload;
import com.cms.dto.OneBookPostingTrackUpdatePayload;
import com.cms.dto.OneBookWebhookResult;
import com.cms.model.Enquiry;
import com.cms.model.OneBookPaymentRequest;
import com.cms.model.enums.CommissionPaymentStatus;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.OneBookPaymentRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OneBookWebhookServiceTest {

    @Mock private OneBookPaymentRequestRepository obRepo;
    @Mock private EnquiryRepository enquiryRepo;
    @Mock private FeeRefundService feeRefundService;
    @Mock private ScholarshipDisbursementService disbursementService;
    @Mock private OneBookConfigService config;
    @Mock private ObjectMapper objectMapper;

    private OneBookWebhookService service;

    @BeforeEach
    void setUp() {
        service = new OneBookWebhookService(
                obRepo, enquiryRepo, feeRefundService, disbursementService, config, objectMapper);
    }

    // ── isValidSecret ──────────────────────────────────────────────────────────

    @Test
    void isValidSecret_returnsTrue_whenSecretMatches() {
        when(config.getWebhookSecret()).thenReturn("my-secret");
        assertThat(service.isValidSecret("my-secret")).isTrue();
    }

    @Test
    void isValidSecret_returnsFalse_whenSecretMismatch() {
        when(config.getWebhookSecret()).thenReturn("my-secret");
        assertThat(service.isValidSecret("wrong")).isFalse();
    }

    @Test
    void isValidSecret_returnsFalse_whenSecretIsNull() {
        when(config.getWebhookSecret()).thenReturn(null);
        assertThat(service.isValidSecret("anything")).isFalse();
    }

    @Test
    void isValidSecret_returnsFalse_whenSecretIsBlank() {
        when(config.getWebhookSecret()).thenReturn("   ");
        assertThat(service.isValidSecret("anything")).isFalse();
    }

    // ── processPostingTrackUpdate ─────────────────────────────────────────────

    @Test
    void postingTrackUpdate_updatesRegisterIdAndStatus_whenFound() {
        OneBookPaymentRequest obReq = obRequest("SCHOLARSHIP", "TRANSMITTED");

        when(obRepo.findByInvoiceNumber("DSB-2026-001")).thenReturn(Optional.of(obReq));
        when(obRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OneBookPostingTrackUpdatePayload payload = new OneBookPostingTrackUpdatePayload(
                "DSB-2026-001", null, "REG-999", "CREATED", null, "noted");

        OneBookWebhookResult result = service.processPostingTrackUpdate(payload, "{}");

        assertThat(result.status()).isEqualTo("OK");
        assertThat(obReq.getOnebookTxnId()).isEqualTo("REG-999");
        assertThat(obReq.getOnebookStatus()).isEqualTo("CREATED");
        assertThat(obReq.getOnebookRemarks()).isEqualTo("noted");
        verify(obRepo).save(obReq);
    }

    @Test
    void postingTrackUpdate_usesDocumentNumber_whenInvoiceNumberIsBlank() {
        OneBookPaymentRequest obReq = obRequest("COMMISSION", "TRANSMITTED");
        when(obRepo.findByInvoiceNumber("COM-2026-001")).thenReturn(Optional.of(obReq));
        when(obRepo.save(any())).thenReturn(obReq);

        OneBookPostingTrackUpdatePayload payload = new OneBookPostingTrackUpdatePayload(
                null, "COM-2026-001", "REG-888", null, null, null);

        OneBookWebhookResult result = service.processPostingTrackUpdate(payload, "{}");

        assertThat(result.status()).isEqualTo("OK");
        assertThat(obReq.getOnebookTxnId()).isEqualTo("REG-888");
    }

    @Test
    void postingTrackUpdate_returnsInvalid_whenBothInvoiceNumbersAreBlank() {
        OneBookPostingTrackUpdatePayload payload = new OneBookPostingTrackUpdatePayload(
                null, "", null, null, null, null);

        OneBookWebhookResult result = service.processPostingTrackUpdate(payload, "{}");

        assertThat(result.status()).isEqualTo("INVALID");
        verify(obRepo, never()).save(any());
    }

    @Test
    void postingTrackUpdate_returnsNotFound_whenInvoiceNumberIsUnknown() {
        when(obRepo.findByInvoiceNumber("UNKNOWN-123")).thenReturn(Optional.empty());

        OneBookPostingTrackUpdatePayload payload = new OneBookPostingTrackUpdatePayload(
                "UNKNOWN-123", null, null, null, null, null);

        OneBookWebhookResult result = service.processPostingTrackUpdate(payload, "{}");

        assertThat(result.status()).isEqualTo("NOT_FOUND");
        verify(obRepo, never()).save(any());
    }

    // ── processPostingTrackCompletion — status mapping ────────────────────────

    @Test
    void postingTrackCompletion_mapsSUCCESSStatusToPAID() {
        OneBookPaymentRequest obReq = obRequest("COMMISSION", "TRANSMITTED");
        obReq.setEntityId(10L);
        Enquiry enquiry = new Enquiry();
        enquiry.setId(10L);
        enquiry.setCommissionPaymentStatus(CommissionPaymentStatus.TRANSMITTED);

        when(obRepo.findByInvoiceNumber("COM-2026-001")).thenReturn(Optional.of(obReq));
        when(obRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(enquiryRepo.findById(10L)).thenReturn(Optional.of(enquiry));
        when(enquiryRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OneBookPostingTrackCompletionPayload payload = completionPayload("COM-2026-001", "SUCCESS");

        OneBookWebhookResult result = service.processPostingTrackCompletion(payload, "{}");

        assertThat(result.status()).isEqualTo("OK");
        assertThat(obReq.getStatus()).isEqualTo("PAID");
        assertThat(enquiry.getCommissionPaymentStatus()).isEqualTo(CommissionPaymentStatus.PAID);
    }

    @Test
    void postingTrackCompletion_mapsREJECTEDStatusToFAILED() {
        OneBookPaymentRequest obReq = obRequest("COMMISSION", "TRANSMITTED");
        obReq.setEntityId(10L);
        Enquiry enquiry = new Enquiry();
        enquiry.setId(10L);
        enquiry.setCommissionPaymentStatus(CommissionPaymentStatus.TRANSMITTED);

        when(obRepo.findByInvoiceNumber("COM-2026-001")).thenReturn(Optional.of(obReq));
        when(obRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(enquiryRepo.findById(10L)).thenReturn(Optional.of(enquiry));
        when(enquiryRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OneBookPostingTrackCompletionPayload payload = completionPayload("COM-2026-001", "REJECTED");

        service.processPostingTrackCompletion(payload, "{}");

        assertThat(obReq.getStatus()).isEqualTo("FAILED");
        assertThat(enquiry.getCommissionPaymentStatus()).isEqualTo(CommissionPaymentStatus.FAILED);
    }

    @Test
    void postingTrackCompletion_mapsUnknownStatusToPROCESSING() {
        OneBookPaymentRequest obReq = obRequest("COMMISSION", "TRANSMITTED");
        obReq.setEntityId(10L);
        Enquiry enquiry = new Enquiry();
        enquiry.setId(10L);
        enquiry.setCommissionPaymentStatus(CommissionPaymentStatus.TRANSMITTED);

        when(obRepo.findByInvoiceNumber("COM-2026-001")).thenReturn(Optional.of(obReq));
        when(obRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(enquiryRepo.findById(10L)).thenReturn(Optional.of(enquiry));
        when(enquiryRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OneBookPostingTrackCompletionPayload payload = completionPayload("COM-2026-001", "IN_PROGRESS");

        service.processPostingTrackCompletion(payload, "{}");

        assertThat(obReq.getStatus()).isEqualTo("PROCESSING");
        assertThat(enquiry.getCommissionPaymentStatus()).isEqualTo(CommissionPaymentStatus.PROCESSING);
    }

    // ── processPostingTrackCompletion — SCHOLARSHIP propagation ───────────────

    @Test
    void postingTrackCompletion_callsCompleteOneBookDisbursement_whenScholarshipPAID() {
        OneBookPaymentRequest obReq = obRequest("SCHOLARSHIP", "TRANSMITTED");
        obReq.setEntityId(42L);
        obReq.setAmount(new BigDecimal("5000"));

        when(obRepo.findByInvoiceNumber("DSB-2026-001")).thenReturn(Optional.of(obReq));
        when(obRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OneBookPostingTrackCompletionPayload payload = completionPayload("DSB-2026-001", "PAID");

        OneBookWebhookResult result = service.processPostingTrackCompletion(payload, "{}");

        assertThat(result.status()).isEqualTo("OK");
        assertThat(obReq.getStatus()).isEqualTo("PAID");
        verify(disbursementService).completeOneBookDisbursement(obReq);
    }

    @Test
    void postingTrackCompletion_doesNotCallDisbursementService_whenScholarshipFAILED() {
        OneBookPaymentRequest obReq = obRequest("SCHOLARSHIP", "TRANSMITTED");
        obReq.setEntityId(42L);

        when(obRepo.findByInvoiceNumber("DSB-2026-001")).thenReturn(Optional.of(obReq));
        when(obRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OneBookPostingTrackCompletionPayload payload = completionPayload("DSB-2026-001", "FAILED");

        OneBookWebhookResult result = service.processPostingTrackCompletion(payload, "{}");

        assertThat(result.status()).isEqualTo("OK");
        assertThat(obReq.getStatus()).isEqualTo("FAILED");
        verify(disbursementService, never()).completeOneBookDisbursement(any());
    }

    // ── processPostingTrackCompletion — REFUND propagation ───────────────────

    @Test
    void postingTrackCompletion_callsCompleteOneBookRefund_whenRefundCompletes() {
        OneBookPaymentRequest obReq = obRequest("REFUND", "TRANSMITTED");
        obReq.setEntityId(99L);

        when(obRepo.findByInvoiceNumber("RFD-2026-001")).thenReturn(Optional.of(obReq));
        when(obRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OneBookPostingTrackCompletionPayload payload = completionPayload("RFD-2026-001", "COMPLETED");

        service.processPostingTrackCompletion(payload, "{}");

        verify(feeRefundService).completeOneBookRefund(obReq, "PAID");
    }

    @Test
    void postingTrackCompletion_callsCompleteOneBookRefund_withFAILED_whenRefundRejected() {
        OneBookPaymentRequest obReq = obRequest("REFUND", "TRANSMITTED");
        obReq.setEntityId(99L);

        when(obRepo.findByInvoiceNumber("RFD-2026-001")).thenReturn(Optional.of(obReq));
        when(obRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OneBookPostingTrackCompletionPayload payload = completionPayload("RFD-2026-001", "CANCELLED");

        service.processPostingTrackCompletion(payload, "{}");

        verify(feeRefundService).completeOneBookRefund(obReq, "FAILED");
    }

    // ── processPostingTrackCompletion — not found ─────────────────────────────

    @Test
    void postingTrackCompletion_returnsNotFound_whenInvoiceUnknown() {
        when(obRepo.findByInvoiceNumber("GHOST-001")).thenReturn(Optional.empty());

        OneBookPostingTrackCompletionPayload payload = completionPayload("GHOST-001", "PAID");

        OneBookWebhookResult result = service.processPostingTrackCompletion(payload, "{}");

        assertThat(result.status()).isEqualTo("NOT_FOUND");
        verify(obRepo, never()).save(any());
    }

    @Test
    void postingTrackCompletion_returnsInvalid_whenBothInvoiceFieldsAreBlank() {
        OneBookPostingTrackCompletionPayload payload = new OneBookPostingTrackCompletionPayload(
                "", null, "PAID", null, null, null, null, null, null, null, null, null);

        OneBookWebhookResult result = service.processPostingTrackCompletion(payload, "{}");

        assertThat(result.status()).isEqualTo("INVALID");
        verify(obRepo, never()).save(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private OneBookPaymentRequest obRequest(String paymentType, String status) {
        OneBookPaymentRequest r = new OneBookPaymentRequest();
        r.setReferenceId("OB-REF-001");
        r.setPaymentType(paymentType);
        r.setInvoiceNumber("DSB-2026-001");
        r.setAmount(new BigDecimal("5000"));
        r.setStatus(status);
        return r;
    }

    private OneBookPostingTrackCompletionPayload completionPayload(String invoiceNumber, String status) {
        return new OneBookPostingTrackCompletionPayload(
                invoiceNumber, null, status, null, null,
                "PAY-001", "SBI", "NEFT", "TXN-001",
                LocalDate.now(), "cashier", "BATCH-1");
    }
}
