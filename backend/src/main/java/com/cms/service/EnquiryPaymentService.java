package com.cms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.EnquiryPaymentRequest;
import com.cms.dto.EnquiryPaymentResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Enquiry;
import com.cms.model.EnquiryPayment;
import com.cms.model.EnquiryStatusHistory;
import com.cms.model.enums.EnquiryStatus;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.EnquiryStatusHistoryRepository;

@Service
@Transactional(readOnly = true)
public class EnquiryPaymentService {

    private final EnquiryPaymentRepository enquiryPaymentRepository;
    private final EnquiryRepository enquiryRepository;
    private final EnquiryStatusHistoryRepository statusHistoryRepository;

    public EnquiryPaymentService(EnquiryPaymentRepository enquiryPaymentRepository,
                                  EnquiryRepository enquiryRepository,
                                  EnquiryStatusHistoryRepository statusHistoryRepository) {
        this.enquiryPaymentRepository = enquiryPaymentRepository;
        this.enquiryRepository = enquiryRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    @Transactional
    public EnquiryPaymentResponse collectPayment(Long enquiryId, EnquiryPaymentRequest request, String collectedBy) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));

        if (enquiry.getStatus() != EnquiryStatus.FEES_FINALIZED
            && enquiry.getStatus() != EnquiryStatus.PARTIALLY_PAID) {
            throw new IllegalStateException(
                "Payment can only be collected when enquiry is in FEES_FINALIZED or PARTIALLY_PAID status. Current: "
                    + enquiry.getStatus()
            );
        }

        String receiptNumber = "RCP-" + LocalDate.now().toString().replace("-", "") + "-"
            + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        EnquiryPayment payment = new EnquiryPayment(
            enquiry,
            request.amountPaid(),
            request.paymentDate(),
            request.paymentMode(),
            request.transactionReference(),
            request.remarks(),
            receiptNumber,
            collectedBy
        );

        EnquiryPayment saved = enquiryPaymentRepository.save(payment);

        BigDecimal totalPaid = enquiryPaymentRepository.sumAmountPaidByEnquiryId(enquiryId);

        EnquiryStatus oldStatus = enquiry.getStatus();
        EnquiryStatus newStatus;
        if (enquiry.getFinalizedNetFee() != null && totalPaid.compareTo(enquiry.getFinalizedNetFee()) >= 0) {
            newStatus = EnquiryStatus.FEES_PAID;
        } else {
            newStatus = EnquiryStatus.PARTIALLY_PAID;
        }

        enquiry.setStatus(newStatus);
        enquiryRepository.save(enquiry);

        statusHistoryRepository.save(new EnquiryStatusHistory(
            enquiry, oldStatus, newStatus, collectedBy, "Payment collected"
        ));

        return toResponse(saved, newStatus);
    }

    public List<EnquiryPaymentResponse> getPaymentsByEnquiryId(Long enquiryId) {
        if (!enquiryRepository.existsById(enquiryId)) {
            throw new ResourceNotFoundException("Enquiry not found with id: " + enquiryId);
        }
        return enquiryPaymentRepository.findByEnquiryIdOrderByPaymentDateDesc(enquiryId).stream()
            .map(p -> toResponse(p, null))
            .toList();
    }

    public EnquiryPaymentResponse getReceipt(Long enquiryId, Long paymentId) {
        EnquiryPayment payment = enquiryPaymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        if (!payment.getEnquiry().getId().equals(enquiryId)) {
            throw new ResourceNotFoundException("Payment " + paymentId + " does not belong to enquiry " + enquiryId);
        }

        return toResponse(payment, null);
    }

    private EnquiryPaymentResponse toResponse(EnquiryPayment payment, EnquiryStatus newStatus) {
        return new EnquiryPaymentResponse(
            payment.getId(),
            payment.getEnquiry().getId(),
            payment.getEnquiry().getName(),
            payment.getAmountPaid(),
            payment.getPaymentDate(),
            payment.getPaymentMode(),
            payment.getTransactionReference(),
            payment.getRemarks(),
            payment.getReceiptNumber(),
            payment.getCollectedBy(),
            newStatus != null ? newStatus.name() : null,
            payment.getCreatedAt()
        );
    }
}
