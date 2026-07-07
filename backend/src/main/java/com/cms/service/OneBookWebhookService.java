package com.cms.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.OneBookPostingTrackCompletionPayload;
import com.cms.dto.OneBookPostingTrackUpdatePayload;
import com.cms.dto.OneBookWebhookResult;
import com.cms.model.Enquiry;
import com.cms.model.OneBookPaymentRequest;
import com.cms.model.enums.CommissionPaymentStatus;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.OneBookPaymentRequestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;

@Service
public class OneBookWebhookService {

    private static final Logger log = LoggerFactory.getLogger(OneBookWebhookService.class);

    private static final Set<String> SUCCESS_STATUSES = Set.of("SUCCESS", "COMPLETED", "PAID");
    private static final Set<String> FAILED_STATUSES  = Set.of("FAILED", "REJECTED", "CANCELLED", "ERROR");

    private final OneBookPaymentRequestRepository obRepo;
    private final EnquiryRepository enquiryRepo;
    private final FeeRefundService feeRefundService;
    private final ScholarshipDisbursementService disbursementService;
    private final OneBookConfigService config;
    private final ObjectMapper objectMapper;

    public OneBookWebhookService(
            OneBookPaymentRequestRepository obRepo,
            EnquiryRepository enquiryRepo,
            FeeRefundService feeRefundService,
            ScholarshipDisbursementService disbursementService,
            OneBookConfigService config,
            ObjectMapper objectMapper) {
        this.obRepo = obRepo;
        this.enquiryRepo = enquiryRepo;
        this.feeRefundService = feeRefundService;
        this.disbursementService = disbursementService;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    public boolean isValidSecret(String headerSecret) {
        String configured = config.getWebhookSecret();
        if (configured == null || configured.isBlank()) {
            log.warn("OneBook webhook secret is not configured — rejecting all webhook calls");
            return false;
        }
        if (headerSecret == null) {
            return false;
        }
        return MessageDigest.isEqual(
                configured.getBytes(StandardCharsets.UTF_8),
                headerSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * OneBook calls this right after accepting a payment register, to deliver
     * the register id it assigned (the real contract has no synchronous id in
     * the create response — it arrives here instead).
     */
    @Transactional
    public OneBookWebhookResult processPostingTrackUpdate(OneBookPostingTrackUpdatePayload payload, String rawJson) {
        String key = payload.correlationKey();
        if (key == null || key.isBlank()) {
            return OneBookWebhookResult.invalid(null, "invoiceNumber or documentNumber is required");
        }

        OneBookPaymentRequest obRequest = obRepo.findByInvoiceNumber(key).orElse(null);
        if (obRequest == null) {
            log.warn("OneBook posting-track-update: no payment request found for invoiceNumber={}", key);
            return OneBookWebhookResult.notFound(key, "No OneBook payment request found for invoiceNumber=" + key);
        }

        if (payload.oneBookPaymentRegisterId() != null) obRequest.setOnebookTxnId(payload.oneBookPaymentRegisterId());
        if (payload.status() != null) obRequest.setOnebookStatus(payload.status());
        if (payload.comment() != null) obRequest.setOnebookRemarks(payload.comment());
        obRequest.setOnebookRawResponse(rawJson);
        obRepo.save(obRequest);

        log.info("OneBook posting-track-update processed. invoiceNumber={} registerId={}",
                key, payload.oneBookPaymentRegisterId());
        return OneBookWebhookResult.ok(key);
    }

    /**
     * OneBook calls this once the payment register's payment is actually
     * completed (or fails) — carries final payment details and triggers
     * propagation to the source entity (commission/refund/scholarship).
     */
    @Transactional
    public OneBookWebhookResult processPostingTrackCompletion(OneBookPostingTrackCompletionPayload payload, String rawJson) {
        String key = payload.correlationKey();
        if (key == null || key.isBlank()) {
            return OneBookWebhookResult.invalid(null, "invoiceNumber or documentNumber is required");
        }

        OneBookPaymentRequest obRequest = obRepo.findByInvoiceNumber(key).orElse(null);
        if (obRequest == null) {
            log.warn("OneBook posting-track-completion: no payment request found for invoiceNumber={}", key);
            return OneBookWebhookResult.notFound(key, "No OneBook payment request found for invoiceNumber=" + key);
        }

        String obStatus = payload.status() != null ? payload.status().toUpperCase() : "UNKNOWN";
        String internalStatus = mapToInternalStatus(obStatus);

        obRequest.setOnebookStatus(payload.status());
        obRequest.setStatus(internalStatus);
        if (payload.transactionNumber() != null) obRequest.setOnebookTxnId(payload.transactionNumber());
        if (payload.paymentDate() != null) obRequest.setOnebookPaidDate(payload.paymentDate());
        if (payload.paymentMode() != null) obRequest.setOnebookPaymentMode(payload.paymentMode());
        if (payload.paymentNumber() != null) obRequest.setOnebookPaymentNumber(payload.paymentNumber());
        if (payload.bankName() != null) obRequest.setOnebookBankName(payload.bankName());
        if (payload.paymentBy() != null) obRequest.setOnebookPaymentBy(payload.paymentBy());
        if (payload.batchNumber() != null) obRequest.setOnebookBatchNumber(payload.batchNumber());
        if (payload.comment() != null) obRequest.setOnebookRemarks(payload.comment());
        obRequest.setOnebookRawResponse(rawJson);
        obRepo.save(obRequest);

        switch (obRequest.getPaymentType()) {
            case "COMMISSION"  -> propagateToCommission(obRequest, internalStatus);
            case "REFUND"      -> feeRefundService.completeOneBookRefund(obRequest, internalStatus);
            case "SCHOLARSHIP" -> {
                if ("PAID".equals(internalStatus)) {
                    disbursementService.completeOneBookDisbursement(obRequest);
                } else if ("FAILED".equals(internalStatus)) {
                    log.warn("OneBook scholarship payment failed. invoiceNumber={} scholarshipId={}",
                            key, obRequest.getEntityId());
                }
            }
            default -> log.warn("Unhandled payment type in webhook: {}", obRequest.getPaymentType());
        }

        log.info("OneBook posting-track-completion processed. invoiceNumber={} obStatus={} internalStatus={}",
                key, payload.status(), internalStatus);
        return OneBookWebhookResult.ok(key);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void propagateToCommission(OneBookPaymentRequest obRequest, String internalStatus) {
        Enquiry enquiry = enquiryRepo.findById(obRequest.getEntityId())
                .orElse(null);
        if (enquiry == null) {
            log.warn("Commission webhook: enquiry {} not found for invoiceNumber={}",
                    obRequest.getEntityId(), obRequest.getInvoiceNumber());
            return;
        }

        CommissionPaymentStatus newStatus = switch (internalStatus) {
            case "PAID"       -> CommissionPaymentStatus.PAID;
            case "PROCESSING" -> CommissionPaymentStatus.PROCESSING;
            case "FAILED"     -> CommissionPaymentStatus.FAILED;
            default           -> CommissionPaymentStatus.PROCESSING;
        };

        enquiry.setCommissionPaymentStatus(newStatus);
        if (newStatus == CommissionPaymentStatus.PAID) {
            enquiry.setCommissionNumber(obRequest.getInvoiceNumber());
        }
        enquiryRepo.save(enquiry);
        log.info("Commission status updated to {} for enquiry={}", newStatus, enquiry.getId());
    }

    private String mapToInternalStatus(String obStatus) {
        if (SUCCESS_STATUSES.contains(obStatus)) return "PAID";
        if (FAILED_STATUSES.contains(obStatus))  return "FAILED";
        return "PROCESSING";
    }

    public String toRawJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
