package com.cms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.UnifiedReceiptResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.FeeRefund;
import com.cms.model.PaymentReceipt;
import com.cms.repository.FeeRefundRepository;
import com.cms.repository.PaymentReceiptRepository;

@Service
@Transactional(readOnly = true)
public class UnifiedReceiptService {

    private final PaymentReceiptRepository receiptRepository;
    private final FeeRefundRepository refundRepository;
    private final ApplicationNumberSequenceService numberSequenceService;

    public UnifiedReceiptService(PaymentReceiptRepository receiptRepository,
                                  FeeRefundRepository refundRepository,
                                  ApplicationNumberSequenceService numberSequenceService) {
        this.receiptRepository = receiptRepository;
        this.refundRepository = refundRepository;
        this.numberSequenceService = numberSequenceService;
    }

    /**
     * Generate the next global sequential receipt number.
     * Format: RCP-YYYY-NNNNN  (e.g. RCP-2026-00001)
     * Uses a pessimistic lock on the receipt_number_sequence row for the current year.
     */
    @Transactional
    public String generateReceiptNumber() {
        return generateReceiptNumber(LocalDate.now().getYear());
    }

    @Transactional
    public String generateReceiptNumber(int year) {
        return numberSequenceService.nextReceiptNumber(year);
    }

    @Transactional
    public String generateRefundNumber() {
        return generateRefundNumber(LocalDate.now().getYear());
    }

    @Transactional
    public String generateRefundNumber(int year) {
        return numberSequenceService.nextRefundNumber(year);
    }

    /**
     * Persist a student payment receipt to the unified table.
     * @param feeCategory TUITION_ONLY | TUITION_AND_HOSTEL
     */
    @Transactional
    public void saveStudentReceipt(String receiptNumber,
                                    Long studentId, String studentName, String rollNumber, String admissionNumber,
                                    String programName, BigDecimal amountPaid,
                                    LocalDate paymentDate, String paymentMode,
                                    String transactionReference, String remarks,
                                    String installmentsCovered, String collectedBy,
                                    String feeCategory) {
        PaymentReceipt receipt = new PaymentReceipt(
            receiptNumber, "STUDENT", studentId,
            studentName, rollNumber, admissionNumber, programName,
            amountPaid, paymentDate, paymentMode,
            transactionReference, remarks, installmentsCovered, collectedBy);
        receipt.setFeeCategory(feeCategory);
        receiptRepository.save(receipt);
    }

    /**
     * Persist an enquiry payment receipt to the unified table.
     * @param feeCategory TUITION_ONLY | TUITION_AND_HOSTEL | null for pre-enrollment
     */
    @Transactional
    public void saveEnquiryReceipt(String receiptNumber,
                                    Long enquiryId, String enquiryName, String programName,
                                    BigDecimal amountPaid, LocalDate paymentDate, String paymentMode,
                                    String transactionReference, String remarks,
                                    String installmentsCovered, String collectedBy,
                                    String feeCategory) {
        PaymentReceipt receipt = new PaymentReceipt(
            receiptNumber, "ENQUIRY", enquiryId,
            enquiryName, null, programName,
            amountPaid, paymentDate, paymentMode,
            transactionReference, remarks, installmentsCovered, collectedBy);
        receipt.setFeeCategory(feeCategory);
        receiptRepository.save(receipt);
    }

    /** Return all receipts (payments + refunds) ordered newest first. */
    public List<UnifiedReceiptResponse> getAllReceipts() {
        List<UnifiedReceiptResponse> merged = new ArrayList<>();
        receiptRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
            .map(r -> toResponse(r, "PAYMENT"))
            .forEach(merged::add);
        refundRepository.findByStatusOrderByCreatedAtDescIdDesc("APPROVED").stream()
            .map(this::toRefundResponse)
            .forEach(merged::add);
        merged.sort(Comparator.comparing(UnifiedReceiptResponse::createdAt).reversed()
            .thenComparingLong(r -> -r.id()));
        return merged;
    }

    /** Return a single receipt by receipt number. */
    public UnifiedReceiptResponse getReceiptByNumber(String receiptNumber) {
        return receiptRepository.findByReceiptNumber(receiptNumber)
            .map(r -> toResponse(r, "PAYMENT"))
            .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + receiptNumber));
    }

    /** Return all receipts for a specific payer. */
    public List<UnifiedReceiptResponse> getReceiptsForPayer(String payerType, Long payerId) {
        return receiptRepository
            .findByPayerTypeAndPayerIdOrderByCreatedAtDesc(payerType, payerId)
            .stream()
            .map(r -> toResponse(r, "PAYMENT"))
            .toList();
    }

    private UnifiedReceiptResponse toResponse(PaymentReceipt r, String receiptType) {
        return new UnifiedReceiptResponse(
            r.getId(), r.getReceiptNumber(),
            r.getPayerType(), r.getPayerId(), r.getPayerName(),
            r.getPayerIdentifier(), r.getAdmissionNumber(), r.getProgramName(),
            r.getAmountPaid(), r.getPaymentDate(), r.getPaymentMode(),
            r.getTransactionReference(), r.getRemarks(),
            r.getInstallmentsCovered(), r.getCollectedBy(), r.getFeeCategory(),
            r.getCreatedAt(), receiptType);
    }

    private UnifiedReceiptResponse toRefundResponse(FeeRefund r) {
        return new UnifiedReceiptResponse(
            r.getId(), r.getRefundNumber(),
            "STUDENT", r.getStudentId(), r.getStudentName(),
            r.getRollNumber(), r.getAdmissionNumber(), r.getProgramName(),
            r.getRefundAmount(), r.getPaymentDate(), r.getPaymentMode(),
            r.getTransactionReference(), r.getReason(),
            "Refund of " + r.getOriginalReceiptNumber(), r.getApprovedBy(), null,
            r.getCreatedAt(), "REFUND");
    }
}

