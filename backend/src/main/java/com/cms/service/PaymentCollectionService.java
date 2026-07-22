package com.cms.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.config.PermSecurityBean;
import com.cms.dto.CollectPaymentRequest;
import com.cms.dto.CollectPaymentResponse;
import com.cms.dto.EnquiryCreditApplicationDto;
import com.cms.dto.ReceiptResponse;
import com.cms.dto.ReceiptSummaryResponse;
import com.cms.dto.SemesterPaymentDetail;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Enquiry;
import com.cms.model.EnquiryCreditApplication;
import com.cms.model.EnquiryPayment;
import com.cms.model.FeeInstallment;
import com.cms.model.FeeRefund;
import com.cms.model.SemesterFee;
import com.cms.model.Student;
import com.cms.model.StudentFeeAllocation;
import com.cms.model.enums.FeeAllocationStatus;
import com.cms.model.enums.PaymentMode;
import com.cms.repository.EnquiryCreditApplicationRepository;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FeeInstallmentRepository;
import com.cms.repository.FeeRefundRepository;
import com.cms.repository.SemesterFeeRepository;
import com.cms.repository.StudentFeeAllocationRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class PaymentCollectionService {

    private final StudentFeeAllocationRepository allocationRepository;
    private final SemesterFeeRepository semesterFeeRepository;
    private final FeeInstallmentRepository installmentRepository;
    private final StudentRepository studentRepository;
    private final EnquiryRepository enquiryRepository;
    private final EnquiryPaymentRepository enquiryPaymentRepository;
    private final FeeRefundRepository refundRepository;
    private final UnifiedReceiptService unifiedReceiptService;
    private final EnquiryCreditApplicationRepository creditApplicationRepository;
    private final TermInstanceService termInstanceService;
    private final FeeRefundService feeRefundService;
    private final PermSecurityBean permSecurityBean;

    public PaymentCollectionService(StudentFeeAllocationRepository allocationRepository,
                                     SemesterFeeRepository semesterFeeRepository,
                                     FeeInstallmentRepository installmentRepository,
                                     StudentRepository studentRepository,
                                     EnquiryRepository enquiryRepository,
                                     EnquiryPaymentRepository enquiryPaymentRepository,
                                     FeeRefundRepository refundRepository,
                                     UnifiedReceiptService unifiedReceiptService,
                                     EnquiryCreditApplicationRepository creditApplicationRepository,
                                     TermInstanceService termInstanceService,
                                     FeeRefundService feeRefundService,
                                     PermSecurityBean permSecurityBean) {
        this.allocationRepository = allocationRepository;
        this.semesterFeeRepository = semesterFeeRepository;
        this.installmentRepository = installmentRepository;
        this.studentRepository = studentRepository;
        this.enquiryRepository = enquiryRepository;
        this.enquiryPaymentRepository = enquiryPaymentRepository;
        this.refundRepository = refundRepository;
        this.unifiedReceiptService = unifiedReceiptService;
        this.creditApplicationRepository = creditApplicationRepository;
        this.termInstanceService = termInstanceService;
        this.feeRefundService = feeRefundService;
        this.permSecurityBean = permSecurityBean;
    }

    @Transactional
    public CollectPaymentResponse collectPayment(Long studentId, CollectPaymentRequest request) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        StudentFeeAllocation allocation = allocationRepository.findByStudentIdForUpdate(studentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Fee allocation not found for student: " + student.getRollNumber()));

        if (allocation.getStatus() != FeeAllocationStatus.FINALIZED) {
            throw new IllegalStateException(
                "Fee allocation is not finalized for student: " + student.getRollNumber());
        }

        List<SemesterFee> semesterFees = semesterFeeRepository
            .findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(allocation.getId());
        int joiningStartYear = termInstanceService.resolveJoiningStartYear(student);

        // Enquiry payments act as pre-payment credit — distribute in installment order before accepting new payment.
        Optional<Enquiry> sourceEnquiry = enquiryRepository.findByConvertedStudentId(studentId);
        BigDecimal totalEnquiryCredit = sourceEnquiry
            .map(e -> enquiryPaymentRepository.sumAmountPaidByEnquiryId(e.getId()))
            .orElse(BigDecimal.ZERO);
        BigDecimal alreadyAppliedCredit = sourceEnquiry
            .map(e -> creditApplicationRepository.sumAmountAppliedByEnquiryId(e.getId()))
            .orElse(BigDecimal.ZERO);
        BigDecimal remainingEnquiryCredit = totalEnquiryCredit.subtract(alreadyAppliedCredit).max(BigDecimal.ZERO);

        BigDecimal collectibleOutstanding = calculateCollectibleOutstanding(
            semesterFees, remainingEnquiryCredit, sourceEnquiry, joiningStartYear);
        if (collectibleOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("No fees are currently due for collection for student: " + student.getRollNumber());
        }
        if (request.amount().compareTo(collectibleOutstanding) > 0) {
            throw new IllegalStateException(
                "Payment amount (" + request.amount() + ") exceeds the amount currently due: " + collectibleOutstanding
            );
        }

        BigDecimal remaining = request.amount();
        List<String> allocationDetails = new ArrayList<>();
        List<SemesterPaymentDetail> installmentBreakdown = new ArrayList<>();
        // Use caller-supplied receipt number (import migration) or auto-generate using the payment date's year
        String receiptNumber = (request.receiptNumber() != null && !request.receiptNumber().isBlank())
            ? request.receiptNumber()
            : unifiedReceiptService.generateReceiptNumber(request.paymentDate().getYear());

        // Source receipt(s) for credit applications — the pre-admission payment receipts, not the new collection receipt
        String creditSourceReceipts = sourceEnquiry
            .map(e -> enquiryPaymentRepository.findByEnquiryIdOrderByPaymentDateDesc(e.getId())
                .stream()
                .sorted(Comparator.comparing(EnquiryPayment::getPaymentDate))
                .map(EnquiryPayment::getReceiptNumber)
                .collect(Collectors.joining(", ")))
            .orElse(receiptNumber);

        for (SemesterFee sf : semesterFees) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            if (!termInstanceService.isSemesterFeeCollectibleNow(joiningStartYear, sf.getYearNumber(), sf.getSemesterSequence())) {
                // If this non-open semester has outstanding balance, block all later semesters —
                // a student cannot collect future-year fees while an earlier year is still due.
                BigDecimal skippedPaid = installmentRepository.sumAmountPaidBySemesterFeeId(sf.getId());
                BigDecimal skippedCredited = sourceEnquiry.isPresent()
                    ? creditApplicationRepository.sumAmountAppliedByEnquiryIdAndSemesterFeeId(
                        sourceEnquiry.get().getId(), sf.getId())
                    : BigDecimal.ZERO;
                if (sf.getAmount().subtract(skippedPaid).subtract(skippedCredited).compareTo(BigDecimal.ZERO) > 0) {
                    break;
                }
                continue;
            }

            BigDecimal alreadyPaid = installmentRepository.sumAmountPaidBySemesterFeeId(sf.getId());
            BigDecimal alreadyCredited = sourceEnquiry.isPresent()
                ? creditApplicationRepository.sumAmountAppliedByEnquiryIdAndSemesterFeeId(
                    sourceEnquiry.get().getId(), sf.getId())
                : BigDecimal.ZERO;
            BigDecimal capacity = sf.getAmount().subtract(alreadyPaid).subtract(alreadyCredited).max(BigDecimal.ZERO);
            BigDecimal creditForThis = remainingEnquiryCredit.min(capacity);
            remainingEnquiryCredit = remainingEnquiryCredit.subtract(creditForThis);

            if (creditForThis.compareTo(BigDecimal.ZERO) > 0 && sourceEnquiry.isPresent()) {
                creditApplicationRepository.save(new EnquiryCreditApplication(
                    sourceEnquiry.get(), student, sf, creditForThis, creditSourceReceipts, Instant.now()));
            }

            BigDecimal pendingForSemester = capacity.subtract(creditForThis).max(BigDecimal.ZERO);

            if (pendingForSemester.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal payForThisSemester = remaining.min(pendingForSemester);

            FeeInstallment installment = new FeeInstallment(
                sf, student, payForThisSemester,
                request.paymentDate(), request.paymentMode(), receiptNumber
            );
            installment.setTransactionReference(request.transactionReference());
            installment.setRemarks(request.remarks());
            installmentRepository.save(installment);

            allocationDetails.add(sf.getSemesterLabel() + ": ₹" + payForThisSemester.toPlainString());
            installmentBreakdown.add(new SemesterPaymentDetail(
                sf.getSemesterLabel(), sf.getYearNumber(), sf.getSemesterSequence(), payForThisSemester
            ));
            remaining = remaining.subtract(payForThisSemester);
        }

        if (allocationDetails.isEmpty()) {
            throw new IllegalStateException("No pending fees found for student: " + student.getRollNumber());
        }

        BigDecimal amountActuallyPaid = request.amount().subtract(remaining);
        String installmentsCovered = installmentBreakdown.stream()
            .map(SemesterPaymentDetail::installmentLabel)
            .collect(Collectors.joining(", "));

        String feeCategory = allocation.isHasHostelFee() ? "TUITION_AND_HOSTEL" : "TUITION_ONLY";

        // Persist to the unified receipts table
        unifiedReceiptService.saveStudentReceipt(
            receiptNumber,
            student.getId(), student.getFullName(), student.getRollNumber(), student.getAdmissionNumber(),
            student.getCourse() != null ? student.getCourse().getName()
                : student.getProgram() != null ? student.getProgram().getName() : null,
            amountActuallyPaid,
            request.paymentDate(), request.paymentMode().name(),
            request.transactionReference(), request.remarks(),
            installmentsCovered, null, feeCategory);

        return new CollectPaymentResponse(
            receiptNumber, student.getId(), student.getFullName(), student.getRollNumber(),
            amountActuallyPaid, request.paymentDate(), request.paymentMode(),
            request.transactionReference(), request.remarks(),
            String.join("; ", allocationDetails),
            installmentBreakdown,
            feeCategory,
            java.time.Instant.now(),
            remaining.max(BigDecimal.ZERO)
        );
    }

    /**
     * Collects a payment without the term gate — the amount can cover any semester (past, current,
     * or future/PLANNED) up to the student's total outstanding balance. Used for advance payments
     * initiated from the per-student Finance tab, not the bulk Collect Payment list.
     */
    @Transactional
    public CollectPaymentResponse collectAdvancePayment(Long studentId, CollectPaymentRequest request) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        StudentFeeAllocation allocation = allocationRepository.findByStudentIdForUpdate(studentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Fee allocation not found for student: " + student.getRollNumber()));

        if (allocation.getStatus() != FeeAllocationStatus.FINALIZED) {
            throw new IllegalStateException(
                "Fee allocation is not finalized for student: " + student.getRollNumber());
        }

        List<SemesterFee> semesterFees = semesterFeeRepository
            .findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(allocation.getId());

        Optional<Enquiry> sourceEnquiry = enquiryRepository.findByConvertedStudentId(studentId);
        BigDecimal totalEnquiryCredit = sourceEnquiry
            .map(e -> enquiryPaymentRepository.sumAmountPaidByEnquiryId(e.getId()))
            .orElse(BigDecimal.ZERO);
        BigDecimal alreadyAppliedCredit = sourceEnquiry
            .map(e -> creditApplicationRepository.sumAmountAppliedByEnquiryId(e.getId()))
            .orElse(BigDecimal.ZERO);
        BigDecimal remainingEnquiryCredit = totalEnquiryCredit.subtract(alreadyAppliedCredit).max(BigDecimal.ZERO);

        BigDecimal totalOutstanding = calculateTotalOutstanding(semesterFees, remainingEnquiryCredit, sourceEnquiry);
        if (totalOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("No outstanding fees for student: " + student.getRollNumber());
        }

        boolean excessRequested = request.amount().compareTo(totalOutstanding) > 0;
        if (excessRequested) {
            if (!request.isAllowExcess()) {
                throw new IllegalStateException(
                    "Payment amount (" + request.amount() + ") exceeds total outstanding: " + totalOutstanding);
            }
            if (request.paymentMode() != PaymentMode.DEMAND_DRAFT && request.paymentMode() != PaymentMode.BANK_TRANSFER) {
                throw new IllegalStateException(
                    "Excess payment is only allowed for Demand Draft or Bank Transfer payments");
            }
            if (!permSecurityBean.has("FEE_COLLECT_EXCESS")) {
                throw new AccessDeniedException("Collecting an excess payment requires the FEE_COLLECT_EXCESS permission");
            }
        }

        BigDecimal remaining = request.amount();
        List<String> allocationDetails = new ArrayList<>();
        List<SemesterPaymentDetail> installmentBreakdown = new ArrayList<>();
        String receiptNumber = (request.receiptNumber() != null && !request.receiptNumber().isBlank())
            ? request.receiptNumber()
            : unifiedReceiptService.generateReceiptNumber(request.paymentDate().getYear());

        String creditSourceReceipts = sourceEnquiry
            .map(e -> enquiryPaymentRepository.findByEnquiryIdOrderByPaymentDateDesc(e.getId())
                .stream()
                .sorted(Comparator.comparing(EnquiryPayment::getPaymentDate))
                .map(EnquiryPayment::getReceiptNumber)
                .collect(Collectors.joining(", ")))
            .orElse(receiptNumber);

        for (SemesterFee sf : semesterFees) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal alreadyPaid = installmentRepository.sumAmountPaidBySemesterFeeId(sf.getId());
            BigDecimal alreadyCredited = sourceEnquiry.isPresent()
                ? creditApplicationRepository.sumAmountAppliedByEnquiryIdAndSemesterFeeId(
                    sourceEnquiry.get().getId(), sf.getId())
                : BigDecimal.ZERO;
            BigDecimal capacity = sf.getAmount().subtract(alreadyPaid).subtract(alreadyCredited).max(BigDecimal.ZERO);
            BigDecimal creditForThis = remainingEnquiryCredit.min(capacity);
            remainingEnquiryCredit = remainingEnquiryCredit.subtract(creditForThis);

            if (creditForThis.compareTo(BigDecimal.ZERO) > 0 && sourceEnquiry.isPresent()) {
                creditApplicationRepository.save(new EnquiryCreditApplication(
                    sourceEnquiry.get(), student, sf, creditForThis, creditSourceReceipts, Instant.now()));
            }

            BigDecimal pendingForSemester = capacity.subtract(creditForThis).max(BigDecimal.ZERO);
            if (pendingForSemester.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal payForThisSemester = remaining.min(pendingForSemester);
            FeeInstallment installment = new FeeInstallment(
                sf, student, payForThisSemester,
                request.paymentDate(), request.paymentMode(), receiptNumber
            );
            installment.setTransactionReference(request.transactionReference());
            installment.setRemarks(request.remarks());
            installmentRepository.save(installment);

            allocationDetails.add(sf.getSemesterLabel() + ": ₹" + payForThisSemester.toPlainString());
            installmentBreakdown.add(new SemesterPaymentDetail(
                sf.getSemesterLabel(), sf.getYearNumber(), sf.getSemesterSequence(), payForThisSemester
            ));
            remaining = remaining.subtract(payForThisSemester);
        }

        if (allocationDetails.isEmpty()) {
            throw new IllegalStateException("No pending fees found for student: " + student.getRollNumber());
        }

        BigDecimal amountActuallyPaid = request.amount().subtract(remaining);
        // With excess allowed, the receipt records the FULL amount physically received (matches
        // the bank/DD reference) — the unallocated portion is carved out as an auto refund below,
        // not silently dropped from the receipt.
        BigDecimal receiptAmount = excessRequested ? request.amount() : amountActuallyPaid;
        String installmentsCovered = installmentBreakdown.stream()
            .map(SemesterPaymentDetail::installmentLabel)
            .collect(Collectors.joining(", "));

        String feeCategory = allocation.isHasHostelFee() ? "TUITION_AND_HOSTEL" : "TUITION_ONLY";

        unifiedReceiptService.saveStudentReceipt(
            receiptNumber,
            student.getId(), student.getFullName(), student.getRollNumber(), student.getAdmissionNumber(),
            student.getCourse() != null ? student.getCourse().getName()
                : student.getProgram() != null ? student.getProgram().getName() : null,
            receiptAmount,
            request.paymentDate(), request.paymentMode().name(),
            request.transactionReference(), request.remarks(),
            installmentsCovered, null, feeCategory);

        if (excessRequested && remaining.compareTo(BigDecimal.ZERO) > 0) {
            feeRefundService.createAutoExcessRefund(student, receiptNumber, remaining);
        }

        return new CollectPaymentResponse(
            receiptNumber, student.getId(), student.getFullName(), student.getRollNumber(),
            receiptAmount, request.paymentDate(), request.paymentMode(),
            request.transactionReference(), request.remarks(),
            String.join("; ", allocationDetails),
            installmentBreakdown,
            feeCategory,
            java.time.Instant.now(),
            remaining.max(BigDecimal.ZERO)
        );
    }

    /**
     * Public entry point for screens (e.g. Collect Payment list) that need the same
     * currently-due figure used at actual collection time, without performing a collection.
     * Returns ZERO for students with no finalized allocation.
     */
    public BigDecimal getCollectibleOutstanding(Student student) {
        StudentFeeAllocation allocation = allocationRepository.findByStudentId(student.getId()).orElse(null);
        if (allocation == null) {
            return BigDecimal.ZERO;
        }

        List<SemesterFee> semesterFees = semesterFeeRepository
            .findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(allocation.getId());
        int joiningStartYear = termInstanceService.resolveJoiningStartYear(student);

        Optional<Enquiry> sourceEnquiry = enquiryRepository.findByConvertedStudentId(student.getId());
        BigDecimal totalEnquiryCredit = sourceEnquiry
            .map(e -> enquiryPaymentRepository.sumAmountPaidByEnquiryId(e.getId()))
            .orElse(BigDecimal.ZERO);
        BigDecimal alreadyAppliedCredit = sourceEnquiry
            .map(e -> creditApplicationRepository.sumAmountAppliedByEnquiryId(e.getId()))
            .orElse(BigDecimal.ZERO);
        BigDecimal remainingEnquiryCredit = totalEnquiryCredit.subtract(alreadyAppliedCredit).max(BigDecimal.ZERO);

        return calculateCollectibleOutstanding(semesterFees, remainingEnquiryCredit, sourceEnquiry, joiningStartYear);
    }

    /**
     * Returns the outstanding amount of just the single next unpaid, currently-open semester fee
     * — the figure a front-desk collector actually asks for right now. Differs from
     * {@link #getCollectibleOutstanding}, which sums EVERY currently-open semester fee (the upper
     * bound one payment may cover, e.g. two already-open semesters paid together).
     * Returns ZERO for students with no finalized allocation.
     */
    public BigDecimal getCurrentInstallmentDue(Student student) {
        StudentFeeAllocation allocation = allocationRepository.findByStudentId(student.getId()).orElse(null);
        if (allocation == null) {
            return BigDecimal.ZERO;
        }

        List<SemesterFee> semesterFees = semesterFeeRepository
            .findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(allocation.getId());
        int joiningStartYear = termInstanceService.resolveJoiningStartYear(student);

        Optional<Enquiry> sourceEnquiry = enquiryRepository.findByConvertedStudentId(student.getId());
        BigDecimal totalEnquiryCredit = sourceEnquiry
            .map(e -> enquiryPaymentRepository.sumAmountPaidByEnquiryId(e.getId()))
            .orElse(BigDecimal.ZERO);
        BigDecimal alreadyAppliedCredit = sourceEnquiry
            .map(e -> creditApplicationRepository.sumAmountAppliedByEnquiryId(e.getId()))
            .orElse(BigDecimal.ZERO);
        BigDecimal remainingCredit = totalEnquiryCredit.subtract(alreadyAppliedCredit).max(BigDecimal.ZERO);

        for (SemesterFee sf : semesterFees) {
            boolean collectibleNow = termInstanceService.isSemesterFeeCollectibleNow(
                joiningStartYear, sf.getYearNumber(), sf.getSemesterSequence());
            BigDecimal alreadyPaid = installmentRepository.sumAmountPaidBySemesterFeeId(sf.getId());
            BigDecimal alreadyCredited = sourceEnquiry.isPresent()
                ? creditApplicationRepository.sumAmountAppliedByEnquiryIdAndSemesterFeeId(
                    sourceEnquiry.get().getId(), sf.getId())
                : BigDecimal.ZERO;
            BigDecimal capacity = sf.getAmount().subtract(alreadyPaid).subtract(alreadyCredited).max(BigDecimal.ZERO);

            if (!collectibleNow) {
                // Mirror calculateCollectibleOutstanding()'s break logic: a non-open semester
                // with real outstanding stops the search — later semesters aren't open either.
                if (capacity.compareTo(BigDecimal.ZERO) > 0) {
                    break;
                }
                continue;
            }

            BigDecimal creditForThis = remainingCredit.min(capacity);
            remainingCredit = remainingCredit.subtract(creditForThis);
            BigDecimal outstanding = capacity.subtract(creditForThis).max(BigDecimal.ZERO);
            if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
                return outstanding;
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Sums outstanding only across installments that are currently open for collection
     * (their TermInstance is OPEN or LOCKED, not still PLANNED) — future terms are excluded
     * from the collectible cap even though they still count toward the student's full balance
     * shown elsewhere (e.g. the Fee Explorer).
     */
    private BigDecimal calculateCollectibleOutstanding(List<SemesterFee> semesterFees,
                                                         BigDecimal netRemainingEnquiryCredit,
                                                         Optional<Enquiry> sourceEnquiry,
                                                         int joiningStartYear) {
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        BigDecimal remainingCredit = netRemainingEnquiryCredit;

        for (SemesterFee sf : semesterFees) {
            // Mirror collectPayment()'s loop exactly: a not-yet-open installment is skipped
            // entirely, including credit consumption, so credit carries forward to the next
            // collectible installment instead of being absorbed by a future, untouched one.
            boolean collectibleNow = termInstanceService.isSemesterFeeCollectibleNow(
                joiningStartYear, sf.getYearNumber(), sf.getSemesterSequence());
            if (!collectibleNow) {
                // Mirror collectPayment()'s break logic: if this non-open semester has outstanding
                // balance, stop counting — later semesters are not collectible either.
                BigDecimal alreadyPaid = installmentRepository.sumAmountPaidBySemesterFeeId(sf.getId());
                BigDecimal alreadyCredited = sourceEnquiry.isPresent()
                    ? creditApplicationRepository.sumAmountAppliedByEnquiryIdAndSemesterFeeId(
                        sourceEnquiry.get().getId(), sf.getId())
                    : BigDecimal.ZERO;
                if (sf.getAmount().subtract(alreadyPaid).subtract(alreadyCredited).compareTo(BigDecimal.ZERO) > 0) {
                    break;
                }
                continue;
            }

            BigDecimal alreadyPaid = installmentRepository.sumAmountPaidBySemesterFeeId(sf.getId());
            BigDecimal alreadyCredited = sourceEnquiry.isPresent()
                ? creditApplicationRepository.sumAmountAppliedByEnquiryIdAndSemesterFeeId(
                    sourceEnquiry.get().getId(), sf.getId())
                : BigDecimal.ZERO;
            BigDecimal capacity = sf.getAmount().subtract(alreadyPaid).subtract(alreadyCredited).max(BigDecimal.ZERO);
            BigDecimal creditForThis = remainingCredit.min(capacity);
            remainingCredit = remainingCredit.subtract(creditForThis);

            totalOutstanding = totalOutstanding.add(capacity.subtract(creditForThis).max(BigDecimal.ZERO));
        }

        return totalOutstanding;
    }

    /** Same as calculateCollectibleOutstanding but without the term-gate — covers all semesters. */
    private BigDecimal calculateTotalOutstanding(List<SemesterFee> semesterFees,
                                                  BigDecimal netRemainingEnquiryCredit,
                                                  Optional<Enquiry> sourceEnquiry) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal remainingCredit = netRemainingEnquiryCredit;

        for (SemesterFee sf : semesterFees) {
            BigDecimal alreadyPaid = installmentRepository.sumAmountPaidBySemesterFeeId(sf.getId());
            BigDecimal alreadyCredited = sourceEnquiry.isPresent()
                ? creditApplicationRepository.sumAmountAppliedByEnquiryIdAndSemesterFeeId(
                    sourceEnquiry.get().getId(), sf.getId())
                : BigDecimal.ZERO;
            BigDecimal capacity = sf.getAmount().subtract(alreadyPaid).subtract(alreadyCredited).max(BigDecimal.ZERO);
            BigDecimal creditForThis = remainingCredit.min(capacity);
            remainingCredit = remainingCredit.subtract(creditForThis);

            total = total.add(capacity.subtract(creditForThis).max(BigDecimal.ZERO));
        }

        return total;
    }

    public List<ReceiptResponse> getReceipts(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }

        List<ReceiptResponse> receipts = installmentRepository
            .findByStudentIdOrderByPaymentDateDesc(studentId).stream()
            .map(this::toReceiptResponse)
            .collect(Collectors.toCollection(ArrayList::new));

        // Approved student refund vouchers
        refundRepository.findByStudentIdAndStatusOrderByPaymentDateDescIdDesc(studentId, "APPROVED")
            .stream()
            .map(r -> toRefundReceiptResponse(r, studentId))
            .forEach(receipts::add);

        // Enquiry-stage payments for the converted student
        enquiryRepository.findByConvertedStudentId(studentId).ifPresent(enquiry -> {
            enquiryPaymentRepository.findByEnquiryIdOrderByPaymentDateDesc(enquiry.getId()).stream()
                .map(p -> toEnquiryPaymentReceiptResponse(p, studentId))
                .forEach(receipts::add);
            refundRepository.findByEnquiryIdAndStatusOrderByPaymentDateDescIdDesc(enquiry.getId(), "APPROVED")
                .stream()
                .map(r -> toRefundReceiptResponse(r, studentId))
                .forEach(receipts::add);
        });

        receipts.sort(
            Comparator.comparing(ReceiptResponse::paymentDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ReceiptResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ReceiptResponse::id, Comparator.nullsLast(Comparator.reverseOrder()))
        );

        return receipts;
    }

    private ReceiptResponse toEnquiryPaymentReceiptResponse(EnquiryPayment p, Long studentId) {
        String feeCategory = p.getEnquiry().getStudentType() == com.cms.model.enums.StudentType.HOSTELER
            ? "TUITION_AND_HOSTEL" : "TUITION_ONLY";
        return new ReceiptResponse(
            p.getId(), p.getReceiptNumber(),
            studentId, p.getEnquiry().getName(), null,
            null, null, null,
            p.getAmountPaid(), p.getPaymentDate(), p.getPaymentMode().name(),
            p.getTransactionReference(), p.getRemarks(), p.getCreatedAt(),
            "ENQUIRY_PAYMENT", null, feeCategory
        );
    }

    private ReceiptResponse toRefundReceiptResponse(FeeRefund refund, Long studentId) {
        return new ReceiptResponse(
            refund.getId(), refund.getRefundNumber(),
            studentId, refund.getStudentName(), refund.getRollNumber(),
            null, null, null,
            refund.getRefundAmount().negate(), refund.getPaymentDate(), refund.getPaymentMode(),
            refund.getTransactionReference(), refund.getReason(), refund.getApprovedAt(),
            "REFUND", refund.getOriginalReceiptNumber(), null
        );
    }

    public ReceiptResponse getReceiptById(Long studentId, Long installmentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
        FeeInstallment installment = installmentRepository.findById(installmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with id: " + installmentId));

        if (!installment.getStudent().getId().equals(studentId)) {
            throw new ResourceNotFoundException("Receipt not found for student with id: " + studentId);
        }

        return toReceiptResponse(installment);
    }

    public List<ReceiptSummaryResponse> getAllReceiptSummaries() {
        Map<String, List<FeeInstallment>> grouped =
            installmentRepository.findAllByOrderByPaymentDateDescIdDesc().stream()
                .collect(Collectors.groupingBy(
                    FeeInstallment::getReceiptNumber, LinkedHashMap::new, Collectors.toList()));

        return grouped.values().stream().map(items -> {
            FeeInstallment first = items.getFirst();

            BigDecimal total = items.stream()
                .map(FeeInstallment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<SemesterPaymentDetail> breakdown = items.stream()
                .map(fi -> new SemesterPaymentDetail(
                    fi.getSemesterFee().getSemesterLabel(),
                    fi.getSemesterFee().getYearNumber(),
                    fi.getSemesterFee().getSemesterSequence(),
                    fi.getAmountPaid()))
                .toList();

            String covered = breakdown.stream()
                .map(SemesterPaymentDetail::installmentLabel)
                .collect(Collectors.joining(", "));

            return new ReceiptSummaryResponse(
                first.getReceiptNumber(),
                first.getStudent().getId(),
                first.getStudent().getFullName(),
                first.getStudent().getRollNumber(),
                total,
                first.getPaymentDate(),
                first.getPaymentMode(),
                first.getTransactionReference(),
                first.getRemarks(),
                covered,
                breakdown,
                first.getCreatedAt());
        }).toList();
    }

    private ReceiptResponse toReceiptResponse(FeeInstallment fi) {
        String feeCategory = fi.getSemesterFee().getAllocation().isHasHostelFee()
            ? "TUITION_AND_HOSTEL" : "TUITION_ONLY";
        return new ReceiptResponse(
            fi.getId(), fi.getReceiptNumber(),
            fi.getStudent().getId(), fi.getStudent().getFullName(), fi.getStudent().getRollNumber(),
            fi.getSemesterFee().getId(), fi.getSemesterFee().getSemesterLabel(),
            fi.getSemesterFee().getYearNumber(),
            fi.getAmountPaid(), fi.getPaymentDate(), fi.getPaymentMode().name(),
            fi.getTransactionReference(), fi.getRemarks(), fi.getCreatedAt(),
            "PAYMENT", null, feeCategory
        );
    }

    public List<EnquiryCreditApplicationDto> getCreditApplicationsByStudent(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
        return creditApplicationRepository.findByStudentIdOrderByAppliedAtDesc(studentId)
            .stream().map(this::toCreditApplicationDto).toList();
    }

    public List<EnquiryCreditApplicationDto> getCreditApplicationsByEnquiry(Long enquiryId) {
        return creditApplicationRepository.findByEnquiryIdOrderByAppliedAtDesc(enquiryId)
            .stream().map(this::toCreditApplicationDto).toList();
    }

    private EnquiryCreditApplicationDto toCreditApplicationDto(EnquiryCreditApplication a) {
        return new EnquiryCreditApplicationDto(
            a.getId(),
            a.getEnquiry().getId(),
            a.getEnquiry().getName(),
            a.getStudent().getId(),
            a.getStudent().getFullName(),
            a.getStudent().getRollNumber(),
            a.getSemesterFee().getId(),
            a.getSemesterFee().getSemesterLabel(),
            a.getAmountApplied(),
            a.getReceiptNumber(),
            a.getAppliedAt()
        );
    }

}
