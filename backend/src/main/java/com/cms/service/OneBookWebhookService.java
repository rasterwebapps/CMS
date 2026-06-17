package com.cms.service;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.OneBookWebhookPayload;
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
        return configured.equals(headerSecret);
    }

    @Transactional
    public void process(OneBookWebhookPayload payload, String rawJson) {
        if (payload.referenceId() == null || payload.referenceId().isBlank()) {
            throw new IllegalArgumentException("referenceId is required in webhook payload");
        }

        OneBookPaymentRequest obRequest = obRepo.findByReferenceId(payload.referenceId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No OneBook payment request found for referenceId: " + payload.referenceId()));

        String obStatus = payload.status() != null ? payload.status().toUpperCase() : "UNKNOWN";
        String internalStatus = mapToInternalStatus(obStatus);

        // Update OneBook tracking fields
        obRequest.setOnebookStatus(payload.status());
        obRequest.setStatus(internalStatus);
        if (payload.transactionId() != null) obRequest.setOnebookTxnId(payload.transactionId());
        if (payload.paidDate() != null) obRequest.setOnebookPaidDate(payload.paidDate());
        if (payload.paymentMode() != null) obRequest.setOnebookPaymentMode(payload.paymentMode());
        if (payload.remarks() != null) obRequest.setOnebookRemarks(payload.remarks());
        obRequest.setOnebookRawResponse(rawJson);
        obRepo.save(obRequest);

        // Propagate status to the source entity
        switch (obRequest.getPaymentType()) {
            case "COMMISSION"  -> propagateToCommission(obRequest, internalStatus);
            case "REFUND"      -> feeRefundService.completeOneBookRefund(obRequest, internalStatus);
            case "SCHOLARSHIP" -> {
                if ("PAID".equals(internalStatus)) {
                    disbursementService.completeOneBookDisbursement(obRequest);
                } else if ("FAILED".equals(internalStatus)) {
                    log.warn("OneBook scholarship payment failed. ref={} scholarshipId={}",
                            obRequest.getReferenceId(), obRequest.getEntityId());
                }
            }
            default -> log.warn("Unhandled payment type in webhook: {}", obRequest.getPaymentType());
        }

        log.info("OneBook webhook processed. ref={} obStatus={} internalStatus={}",
                payload.referenceId(), payload.status(), internalStatus);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void propagateToCommission(OneBookPaymentRequest obRequest, String internalStatus) {
        Enquiry enquiry = enquiryRepo.findById(obRequest.getEntityId())
                .orElse(null);
        if (enquiry == null) {
            log.warn("Commission webhook: enquiry {} not found for ref={}", obRequest.getEntityId(), obRequest.getReferenceId());
            return;
        }

        CommissionPaymentStatus newStatus = switch (internalStatus) {
            case "PAID"       -> CommissionPaymentStatus.PAID;
            case "PROCESSING" -> CommissionPaymentStatus.PROCESSING;
            case "FAILED"     -> CommissionPaymentStatus.FAILED;
            default           -> CommissionPaymentStatus.PROCESSING;
        };

        enquiry.setCommissionPaymentStatus(newStatus);
        enquiryRepo.save(enquiry);
        log.info("Commission status updated to {} for enquiry={}", newStatus, enquiry.getId());
    }

    private String mapToInternalStatus(String obStatus) {
        if (SUCCESS_STATUSES.contains(obStatus)) return "PAID";
        if (FAILED_STATUSES.contains(obStatus))  return "FAILED";
        return "PROCESSING";
    }

    public String toRawJson(OneBookWebhookPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
