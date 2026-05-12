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

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Service
@Transactional(readOnly = true)
public class UnifiedReceiptService {

    private final PaymentReceiptRepository receiptRepository;
    private final EntityManager entityManager;

    public UnifiedReceiptService(PaymentReceiptRepository receiptRepository,
                                  EntityManager entityManager) {
        this.receiptRepository = receiptRepository;
        this.entityManager = entityManager;
    }

    /**
     * Generate the next global sequential receipt number.
     * Format: RCP-YYYY-NNNNN  (e.g. RCP-2026-00001)
     * Uses a pessimistic lock on the receipt_number_sequence row for the current year.
     */
    @Transactional
    public String generateReceiptNumber() {
        int year = LocalDate.now().getYear();

        // Upsert the sequence row with a SELECT FOR UPDATE so concurrent calls stay ordered
        var result = entityManager.createNativeQuery(
            """
            INSERT INTO receipt_number_sequence (year, last_seq)
            VALUES (:year, 1)
            ON CONFLICT (year) DO UPDATE
                SET last_seq = receipt_number_sequence.last_seq + 1
            RETURNING last_seq
            """)
            .setParameter("year", year)
            .getSingleResult();

        int seq = ((Number) result).intValue();
        return String.format("RCP-%d-%05d", year, seq);
    }

    /**
     * Persist a student payment receipt to the unified table.
     */
    @Transactional
    public void saveStudentReceipt(String receiptNumber,
                                    Long studentId, String studentName, String rollNumber,
                                    String programName, BigDecimal amountPaid,
                                    LocalDate paymentDate, String paymentMode,
                                    String transactionReference, String remarks,
                                    String installmentsCovered, String collectedBy) {
        PaymentReceipt receipt = new PaymentReceipt(
            receiptNumber, "STUDENT", studentId,
            studentName, rollNumber, programName,
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
            r.getPayerIdentifier(), r.getProgramName(),
            r.getAmountPaid(), r.getPaymentDate(), r.getPaymentMode(),
            r.getTransactionReference(), r.getRemarks(),
            r.getInstallmentsCovered(), r.getCollectedBy(), r.getCreatedAt());
    }
}

