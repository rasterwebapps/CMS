package com.cms.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.FeeRefundApprovalRequest;
import com.cms.dto.FeeRefundRejectionRequest;
import com.cms.dto.FeeRefundRequest;
import com.cms.dto.FeeRefundResponse;
import com.cms.dto.FeeRefundSummaryResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.EnquiryPayment;
import com.cms.model.FeeInstallment;
import com.cms.model.FeeRefund;
import com.cms.model.PaymentReceipt;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FeeInstallmentRepository;
import com.cms.repository.FeeRefundRepository;
import com.cms.repository.PaymentReceiptRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class FeeRefundService {

    private final StudentRepository studentRepository;
    private final EnquiryRepository enquiryRepository;
    private final PaymentReceiptRepository receiptRepository;
    private final FeeInstallmentRepository installmentRepository;
    private final EnquiryPaymentRepository enquiryPaymentRepository;
    private final FeeRefundRepository refundRepository;
    private final UnifiedReceiptService unifiedReceiptService;

    public FeeRefundService(StudentRepository studentRepository,
                             EnquiryRepository enquiryRepository,
                             PaymentReceiptRepository receiptRepository,
                             FeeInstallmentRepository installmentRepository,
                             EnquiryPaymentRepository enquiryPaymentRepository,
                             FeeRefundRepository refundRepository,
                             UnifiedReceiptService unifiedReceiptService) {
        this.studentRepository = studentRepository;
        this.enquiryRepository = enquiryRepository;
        this.receiptRepository = receiptRepository;
        this.installmentRepository = installmentRepository;
        this.enquiryPaymentRepository = enquiryPaymentRepository;
        this.refundRepository = refundRepository;
        this.unifiedReceiptService = unifiedReceiptService;
    }

    /**
     * Unified Step 1 — initiate a refund request for any receipt (STUDENT or ENQUIRY).
     * Looks up the entity type from the unified receipt ledger; routes to the correct path.
     * Creates a PENDING record; does NOT touch payment rows yet.
     */
    @Transactional
    public FeeRefundResponse initiateRefund(FeeRefundRequest request, String requestedBy) {
        PaymentReceipt receipt = receiptRepository.findByReceiptNumber(request.receiptNumber())
            .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + request.receiptNumber()));

        if (refundRepository.existsByOriginalReceiptNumberAndStatusNot(request.receiptNumber(), "REJECTED")) {
            throw new IllegalStateException("An active refund request already exists for receipt: " + request.receiptNumber());
        }

        return switch (receipt.getPayerType()) {
            case "STUDENT" -> initiateStudentRefund(receipt, request, requestedBy);
            case "ENQUIRY" -> initiateEnquiryRefund(receipt, request, requestedBy);
            default -> throw new IllegalArgumentException("Unsupported payer type: " + receipt.getPayerType());
        };
    }

    private FeeRefundResponse initiateStudentRefund(PaymentReceipt receipt,
                                                     FeeRefundRequest request,
                                                     String requestedBy) {
        var student = studentRepository.findById(receipt.getPayerId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + receipt.getPayerId()));

        List<FeeInstallment> installments = installmentRepository.findByReceiptNumber(request.receiptNumber());
        if (installments.isEmpty()) {
            throw new IllegalStateException("No installment records found for receipt: " + request.receiptNumber());
        }

        BigDecimal refundAmount = installments.stream()
            .map(FeeInstallment::getAmountPaid)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        String programName = student.getCourse() != null ? student.getCourse().getName()
            : student.getProgram() != null ? student.getProgram().getName() : null;

        FeeRefund refund = new FeeRefund();
        refund.setEntityType("STUDENT");
        refund.setOriginalReceiptNumber(request.receiptNumber());
        refund.setStudentId(receipt.getPayerId());
        refund.setStudentName(student.getFullName());
        refund.setRollNumber(student.getRollNumber());
        refund.setAdmissionNumber(student.getAdmissionNumber());
        refund.setProgramName(programName);
        refund.setRefundAmount(refundAmount);
        refund.setReason(request.reason());
        refund.setStatus("PENDING");
        refund.setRequestedBy(requestedBy);
        refund.setRequestedAt(Instant.now());

        FeeRefund saved = refundRepository.save(refund);
        return new FeeRefundResponse(
            saved.getId(), request.receiptNumber(),
            refundAmount, request.reason(),
            student.getFullName(), student.getRollNumber(), "PENDING");
    }

    private FeeRefundResponse initiateEnquiryRefund(PaymentReceipt receipt,
                                                      FeeRefundRequest request,
                                                      String requestedBy) {
        var enquiry = enquiryRepository.findById(receipt.getPayerId())
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found: " + receipt.getPayerId()));

        EnquiryPayment payment = enquiryPaymentRepository.findByReceiptNumber(request.receiptNumber())
            .orElseThrow(() -> new IllegalStateException("No payment record found for receipt: " + request.receiptNumber()));

        if (payment.getRefundedAt() != null) {
            throw new IllegalStateException("This enquiry payment has already been refunded");
        }

        FeeRefund refund = new FeeRefund();
        refund.setEntityType("ENQUIRY");
        refund.setOriginalReceiptNumber(request.receiptNumber());
        refund.setEnquiryId(enquiry.getId());
        refund.setStudentName(enquiry.getName());
        refund.setRefundAmount(payment.getAmountPaid());
        refund.setReason(request.reason());
        refund.setStatus("PENDING");
        refund.setRequestedBy(requestedBy);
        refund.setRequestedAt(Instant.now());

        FeeRefund saved = refundRepository.save(refund);
        return new FeeRefundResponse(
            saved.getId(), request.receiptNumber(),
            payment.getAmountPaid(), request.reason(),
            enquiry.getName(), null, "PENDING");
    }

    /** Step 2a — approver confirms the refund and records how the money was returned. */
    @Transactional
    public FeeRefundSummaryResponse approveRefund(Long refundId,
                                                   FeeRefundApprovalRequest request,
                                                   String approvedBy) {
        FeeRefund refund = refundRepository.findById(refundId)
            .orElseThrow(() -> new ResourceNotFoundException("Refund request not found: " + refundId));
        if (!"PENDING".equals(refund.getStatus())) {
            throw new IllegalStateException("Only PENDING refund requests can be approved");
        }

        LocalDate paymentDate = request.paymentDate() != null ? request.paymentDate() : LocalDate.now();
        String refundNumber = unifiedReceiptService.generateRefundNumber(paymentDate.getYear());
        Instant now = Instant.now();

        if ("ENQUIRY".equals(refund.getEntityType())) {
            // Soft-flag the enquiry payment row so outstanding queries exclude it
            EnquiryPayment payment = enquiryPaymentRepository
                .findByReceiptNumber(refund.getOriginalReceiptNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Payment not found for receipt: " + refund.getOriginalReceiptNumber()));
            payment.setRefundedAt(now);
            payment.setRefundNumber(refundNumber);
            enquiryPaymentRepository.save(payment);
        } else {
            // Soft-flag the fee_installments so they are excluded from outstanding calculations
            List<FeeInstallment> installments =
                installmentRepository.findByReceiptNumber(refund.getOriginalReceiptNumber());
            installments.forEach(fi -> {
                fi.setRefundedAt(now);
                fi.setRefundNumber(refundNumber);
            });
            installmentRepository.saveAll(installments);
        }

        refund.setStatus("APPROVED");
        refund.setRefundNumber(refundNumber);
        refund.setPaymentMode(request.paymentMode());
        refund.setPaymentDate(paymentDate);
        refund.setTransactionReference(request.transactionReference());
        refund.setApprovedBy(approvedBy);
        refund.setApprovedAt(now);

        return toSummaryResponse(refundRepository.save(refund));
    }

    /** Step 2b — approver rejects the refund request. No payment rows are touched. */
    @Transactional
    public FeeRefundSummaryResponse rejectRefund(Long refundId,
                                                  FeeRefundRejectionRequest request,
                                                  String rejectedBy) {
        FeeRefund refund = refundRepository.findById(refundId)
            .orElseThrow(() -> new ResourceNotFoundException("Refund request not found: " + refundId));
        if (!"PENDING".equals(refund.getStatus())) {
            throw new IllegalStateException("Only PENDING refund requests can be rejected");
        }

        refund.setStatus("REJECTED");
        refund.setRejectionReason(request.rejectionReason());
        refund.setApprovedBy(rejectedBy);
        refund.setApprovedAt(Instant.now());

        return toSummaryResponse(refundRepository.save(refund));
    }

    public List<FeeRefundSummaryResponse> getPendingRefunds() {
        return refundRepository.findByStatusOrderByRequestedAtDescIdDesc("PENDING")
            .stream()
            .map(this::toSummaryResponse)
            .toList();
    }

    public List<FeeRefundSummaryResponse> getAllRefunds() {
        return refundRepository.findAllOrderedByStatusAndDate()
            .stream()
            .map(this::toSummaryResponse)
            .toList();
    }

    private FeeRefundSummaryResponse toSummaryResponse(FeeRefund r) {
        return new FeeRefundSummaryResponse(
            r.getId(), r.getOriginalReceiptNumber(),
            r.getEntityType() != null ? r.getEntityType() : "STUDENT",
            r.getStudentName(), r.getRollNumber(), r.getAdmissionNumber(), r.getProgramName(),
            r.getRefundAmount(), r.getReason(), r.getRequestedBy(),
            r.getRequestedAt() != null ? r.getRequestedAt().toString() : null,
            r.getStatus(),
            r.getRefundNumber(),
            r.getPaymentMode(),
            r.getPaymentDate() != null ? r.getPaymentDate().toString() : null,
            r.getTransactionReference(),
            r.getApprovedBy(),
            r.getApprovedAt() != null ? r.getApprovedAt().toString() : null,
            r.getRejectionReason());
    }
}
