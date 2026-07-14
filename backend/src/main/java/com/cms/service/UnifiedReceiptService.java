package com.cms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.UnifiedReceiptResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Enquiry;
import com.cms.model.FeeRefund;
import com.cms.model.PaymentReceipt;
import com.cms.model.Student;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FeeRefundRepository;
import com.cms.repository.PaymentReceiptRepository;
import com.cms.repository.PaymentReceiptSpecification;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class UnifiedReceiptService {

    /** id + name of the academic year a STUDENT or ENQUIRY payer is tagged against. */
    private record YearTag(Long id, String name) {
        static final YearTag NONE = new YearTag(null, null);
    }

    private final PaymentReceiptRepository receiptRepository;
    private final FeeRefundRepository refundRepository;
    private final ApplicationNumberSequenceService numberSequenceService;
    private final StudentRepository studentRepository;
    private final EnquiryRepository enquiryRepository;

    public UnifiedReceiptService(PaymentReceiptRepository receiptRepository,
                                  FeeRefundRepository refundRepository,
                                  ApplicationNumberSequenceService numberSequenceService,
                                  StudentRepository studentRepository,
                                  EnquiryRepository enquiryRepository) {
        this.receiptRepository = receiptRepository;
        this.refundRepository = refundRepository;
        this.numberSequenceService = numberSequenceService;
        this.studentRepository = studentRepository;
        this.enquiryRepository = enquiryRepository;
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

    public Page<UnifiedReceiptResponse> getPaymentsPage(
            String search, String paymentMode, String payerType,
            LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        Specification<PaymentReceipt> spec = Specification.where(null);
        if (search != null && search.length() >= 2)   spec = spec.and(PaymentReceiptSpecification.bySearch(search));
        if (paymentMode != null && !paymentMode.isBlank()) spec = spec.and(PaymentReceiptSpecification.byPaymentMode(paymentMode));
        if (payerType != null && !payerType.isBlank())     spec = spec.and(PaymentReceiptSpecification.byPayerType(payerType));
        if (fromDate != null)                              spec = spec.and(PaymentReceiptSpecification.byDateFrom(fromDate));
        if (toDate != null)                                spec = spec.and(PaymentReceiptSpecification.byDateTo(toDate));

        Page<PaymentReceipt> page = receiptRepository.findAll(spec, pageable);
        Map<String, String> refundStatusByReceipt = getActiveRefundStatusByReceipt();

        List<Long> payerIds = page.getContent().stream().map(PaymentReceipt::getPayerId).distinct().toList();
        List<Long> studentIds = page.getContent().stream()
            .filter(r -> "STUDENT".equals(r.getPayerType())).map(PaymentReceipt::getPayerId).distinct().toList();
        List<Long> enquiryIds = page.getContent().stream()
            .filter(r -> "ENQUIRY".equals(r.getPayerType())).map(PaymentReceipt::getPayerId).distinct().toList();
        Map<Long, YearTag> studentYears = resolveStudentYears(studentIds.stream());
        Map<Long, YearTag> enquiryYears = resolveEnquiryYears(enquiryIds.stream());

        List<UnifiedReceiptResponse> content = page.getContent().stream()
            .map(r -> toResponse(r, "PAYMENT", refundStatusByReceipt.get(r.getReceiptNumber()), studentYears, enquiryYears))
            .toList();
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    /** Unbounded export: same spec as paginated list, returns all matching payment receipts. */
    public List<UnifiedReceiptResponse> getPaymentsAll(
            String search, String paymentMode, String payerType,
            LocalDate fromDate, LocalDate toDate, Sort sort) {
        Specification<PaymentReceipt> spec = Specification.where(null);
        if (search != null && search.length() >= 2)        spec = spec.and(PaymentReceiptSpecification.bySearch(search));
        if (paymentMode != null && !paymentMode.isBlank()) spec = spec.and(PaymentReceiptSpecification.byPaymentMode(paymentMode));
        if (payerType != null && !payerType.isBlank())     spec = spec.and(PaymentReceiptSpecification.byPayerType(payerType));
        if (fromDate != null)                              spec = spec.and(PaymentReceiptSpecification.byDateFrom(fromDate));
        if (toDate != null)                                spec = spec.and(PaymentReceiptSpecification.byDateTo(toDate));

        List<PaymentReceipt> receipts = receiptRepository.findAll(spec, sort);
        Map<String, String> refundStatusByReceipt = getActiveRefundStatusByReceipt();
        List<Long> studentIds = receipts.stream()
            .filter(r -> "STUDENT".equals(r.getPayerType())).map(PaymentReceipt::getPayerId).distinct().toList();
        List<Long> enquiryIds = receipts.stream()
            .filter(r -> "ENQUIRY".equals(r.getPayerType())).map(PaymentReceipt::getPayerId).distinct().toList();
        Map<Long, YearTag> studentYears = resolveStudentYears(studentIds.stream());
        Map<Long, YearTag> enquiryYears = resolveEnquiryYears(enquiryIds.stream());
        return receipts.stream()
            .map(r -> toResponse(r, "PAYMENT", refundStatusByReceipt.get(r.getReceiptNumber()), studentYears, enquiryYears))
            .toList();
    }

    /** Return all receipts (payments + refunds) ordered newest first. */
    public List<UnifiedReceiptResponse> getAllReceipts() {
        List<UnifiedReceiptResponse> merged = new ArrayList<>();
        Map<String, String> refundStatusByReceipt = getActiveRefundStatusByReceipt();

        List<PaymentReceipt> receipts = receiptRepository.findAllByOrderByCreatedAtDescIdDesc();
        List<FeeRefund> refunds = refundRepository.findByStatusOrderByCreatedAtDescIdDesc("APPROVED");

        Map<Long, YearTag> studentYears = resolveStudentYears(Stream.concat(
            receipts.stream().filter(r -> "STUDENT".equals(r.getPayerType())).map(PaymentReceipt::getPayerId),
            refunds.stream().filter(r -> !"ENQUIRY".equals(r.getEntityType())).map(FeeRefund::getStudentId)));
        Map<Long, YearTag> enquiryYears = resolveEnquiryYears(Stream.concat(
            receipts.stream().filter(r -> "ENQUIRY".equals(r.getPayerType())).map(PaymentReceipt::getPayerId),
            refunds.stream().filter(r -> "ENQUIRY".equals(r.getEntityType())).map(FeeRefund::getEnquiryId)));

        receipts.stream()
            .map(r -> toResponse(r, "PAYMENT", refundStatusByReceipt.get(r.getReceiptNumber()), studentYears, enquiryYears))
            .forEach(merged::add);

        refunds.stream()
            .map(r -> toRefundResponse(r, studentYears, enquiryYears))
            .forEach(merged::add);

        merged.sort(Comparator.comparing(UnifiedReceiptResponse::createdAt).reversed()
            .thenComparingLong(r -> -r.id()));
        return merged;
    }

    /** Return a single receipt by receipt number. */
    public UnifiedReceiptResponse getReceiptByNumber(String receiptNumber) {
        String refundStatus = getActiveRefundStatusByReceipt().get(receiptNumber);
        return receiptRepository.findByReceiptNumber(receiptNumber)
            .map(r -> toResponse(r, "PAYMENT", refundStatus, resolveYearTag(r.getPayerType(), r.getPayerId())))
            .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + receiptNumber));
    }

    /** Return all receipts for a specific payer. */
    public List<UnifiedReceiptResponse> getReceiptsForPayer(String payerType, Long payerId) {
        Map<String, String> refundStatusByReceipt = getActiveRefundStatusByReceipt();
        YearTag yearTag = resolveYearTag(payerType, payerId);
        return receiptRepository
            .findByPayerTypeAndPayerIdOrderByCreatedAtDesc(payerType, payerId)
            .stream()
            .map(r -> toResponse(r, "PAYMENT", refundStatusByReceipt.get(r.getReceiptNumber()), yearTag))
            .toList();
    }

    private Map<String, String> getActiveRefundStatusByReceipt() {
        Map<String, String> statusByReceipt = new HashMap<>();
        refundRepository.findByStatusIn(List.of("PENDING", "APPROVED")).forEach(refund -> {
            // Prefer PENDING if stale duplicates exist from older data.
            statusByReceipt.merge(
                refund.getOriginalReceiptNumber(),
                refund.getStatus(),
                (current, incoming) -> "PENDING".equals(current) || "PENDING".equals(incoming) ? "PENDING" : incoming
            );
        });
        return statusByReceipt;
    }

    /** A student's academic year is the admission year of the cohort they belong to. */
    private Map<Long, YearTag> resolveStudentYears(Stream<Long> studentIds) {
        List<Long> ids = studentIds.filter(java.util.Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return studentRepository.findByIdInWithRelations(ids).stream()
            .collect(Collectors.toMap(Student::getId, s -> s.getCohort() != null && s.getCohort().getAdmissionAcademicYear() != null
                ? new YearTag(s.getCohort().getAdmissionAcademicYear().getId(), s.getCohort().getAdmissionAcademicYear().getName())
                : YearTag.NONE));
    }

    /** An enquiry's academic year is whatever it's tagged with (auto-derived or manually set). */
    private Map<Long, YearTag> resolveEnquiryYears(Stream<Long> enquiryIds) {
        List<Long> ids = enquiryIds.filter(java.util.Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return enquiryRepository.findByIdInWithAcademicYear(ids).stream()
            .collect(Collectors.toMap(Enquiry::getId, e -> e.getAcademicYear() != null
                ? new YearTag(e.getAcademicYear().getId(), e.getAcademicYear().getName())
                : YearTag.NONE));
    }

    private YearTag resolveYearTag(String payerType, Long payerId) {
        if (payerId == null) return YearTag.NONE;
        if ("ENQUIRY".equals(payerType)) {
            return resolveEnquiryYears(Stream.of(payerId)).getOrDefault(payerId, YearTag.NONE);
        }
        return resolveStudentYears(Stream.of(payerId)).getOrDefault(payerId, YearTag.NONE);
    }

    private UnifiedReceiptResponse toResponse(PaymentReceipt r, String receiptType, String refundStatus,
                                               Map<Long, YearTag> studentYears, Map<Long, YearTag> enquiryYears) {
        Map<Long, YearTag> years = "ENQUIRY".equals(r.getPayerType()) ? enquiryYears : studentYears;
        return toResponse(r, receiptType, refundStatus, years.getOrDefault(r.getPayerId(), YearTag.NONE));
    }

    private UnifiedReceiptResponse toResponse(PaymentReceipt r, String receiptType, String refundStatus, YearTag yearTag) {
        return new UnifiedReceiptResponse(
            r.getId(), r.getReceiptNumber(),
            r.getPayerType(), r.getPayerId(), r.getPayerName(),
            r.getPayerIdentifier(), r.getAdmissionNumber(), r.getProgramName(),
            yearTag.id(), yearTag.name(),
            r.getAmountPaid(), r.getPaymentDate(), r.getPaymentMode(),
            r.getTransactionReference(), r.getRemarks(),
            r.getInstallmentsCovered(), r.getCollectedBy(), r.getFeeCategory(),
            r.getCreatedAt(), receiptType,
            "APPROVED".equals(refundStatus),
            refundStatus);
    }

    private UnifiedReceiptResponse toRefundResponse(FeeRefund r, Map<Long, YearTag> studentYears, Map<Long, YearTag> enquiryYears) {
        String entityType = r.getEntityType() != null ? r.getEntityType() : "STUDENT";
        Long payerId = "ENQUIRY".equals(entityType) ? r.getEnquiryId() : r.getStudentId();
        Map<Long, YearTag> years = "ENQUIRY".equals(entityType) ? enquiryYears : studentYears;
        YearTag yearTag = years.getOrDefault(payerId, YearTag.NONE);
        return new UnifiedReceiptResponse(
            r.getId(), r.getRefundNumber(),
            entityType, payerId, r.getStudentName(),
            r.getRollNumber(), r.getAdmissionNumber(), r.getProgramName(),
            yearTag.id(), yearTag.name(),
            r.getRefundAmount(), r.getPaymentDate(), r.getPaymentMode(),
            r.getTransactionReference(), r.getReason(),
            "Refund of " + r.getOriginalReceiptNumber(), r.getApprovedBy(), null,
            r.getCreatedAt(), "REFUND",
            false,
            null);
    }
}
