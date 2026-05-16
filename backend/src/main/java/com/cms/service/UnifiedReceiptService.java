package com.cms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.UnifiedReceiptResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.PaymentReceipt;
import com.cms.repository.PaymentReceiptRepository;

@Service
@Transactional(readOnly = true)
public class UnifiedReceiptService {

    private final PaymentReceiptRepository receiptRepository;
    private final ApplicationNumberSequenceService numberSequenceService;

    public UnifiedReceiptService(PaymentReceiptRepository receiptRepository,
                                  ApplicationNumberSequenceService numberSequenceService) {
        this.receiptRepository = receiptRepository;
        this.numberSequenceService = numberSequenceService;
    }

    /**
     * Generate the next global sequential receipt number.
     * Format: RCP-YYYY-NNNNN  (e.g. RCP-2026-00001)
     * Uses a pessimistic lock on the receipt_number_sequence row for the current year.
     */
    @Transactional
    public String generateReceiptNumber() {
        int year = LocalDate.now().getYear();
        return numberSequenceService.nextReceiptNumber(year);
    }

    /**
     * Persist a student payment receipt to the unified table.
     */
    @Transactional
    public void saveStudentReceipt(String receiptNumber,
                                    Long studentId, String studentName, String rollNumber, String admissionNumber,
                                    String programName, BigDecimal amountPaid,
                                    LocalDate paymentDate, String paymentMode,
                                    String transactionReference, String remarks,
                                    String installmentsCovered, String collectedBy) {
        PaymentReceipt receipt = new PaymentReceipt(
            receiptNumber, "STUDENT", studentId,
            studentName, rollNumber, admissionNumber, programName,
            amountPaid, paymentDate, paymentMode,
            transactionReference, remarks, installmentsCovered, collectedBy);
        receiptRepository.save(receipt);
    }

    /**
     * Persist an enquiry payment receipt to the unified table.
     */
    @Transactional
    public void saveEnquiryReceipt(String receiptNumber,
                                    Long enquiryId, String enquiryName, String programName,
                                    BigDecimal amountPaid, LocalDate paymentDate, String paymentMode,
                                    String transactionReference, String remarks,
                                    String installmentsCovered, String collectedBy) {
        PaymentReceipt receipt = new PaymentReceipt(
            receiptNumber, "ENQUIRY", enquiryId,
            enquiryName, null, programName,
            amountPaid, paymentDate, paymentMode,
            transactionReference, remarks, installmentsCovered, collectedBy);
        receiptRepository.save(receipt);
    }

    /** Return all receipts ordered newest first. */
    public List<UnifiedReceiptResponse> getAllReceipts() {
        return receiptRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
            .map(this::toResponse)
            .toList();
    }

    /** Return a single receipt by receipt number. */
    public UnifiedReceiptResponse getReceiptByNumber(String receiptNumber) {
        return receiptRepository.findByReceiptNumber(receiptNumber)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + receiptNumber));
    }

    /** Return all receipts for a specific payer. */
    public List<UnifiedReceiptResponse> getReceiptsForPayer(String payerType, Long payerId) {
        return receiptRepository
            .findByPayerTypeAndPayerIdOrderByCreatedAtDesc(payerType, payerId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private UnifiedReceiptResponse toResponse(PaymentReceipt r) {
        return new UnifiedReceiptResponse(
            r.getId(), r.getReceiptNumber(),
            r.getPayerType(), r.getPayerId(), r.getPayerName(),
            r.getPayerIdentifier(), r.getAdmissionNumber(), r.getProgramName(),
            r.getAmountPaid(), r.getPaymentDate(), r.getPaymentMode(),
            r.getTransactionReference(), r.getRemarks(),
            r.getInstallmentsCovered(), r.getCollectedBy(), r.getCreatedAt());
    }
}

