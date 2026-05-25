package com.cms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.EnquiryPaymentRequest;
import com.cms.dto.EnquiryPaymentResponse;
import com.cms.dto.EnquiryYearWiseFeeStatusResponse;
import com.cms.dto.EnquiryYearWiseFeeStatusResponse.InstallmentFeeStatus;
import com.cms.dto.EnquiryYearWiseFeeStatusResponse.YearFeeStatus;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Enquiry;
import com.cms.model.EnquiryPayment;
import com.cms.model.EnquiryStatusHistory;
import com.cms.model.TermBillingSchedule;
import com.cms.model.enums.EnquiryStatus;
import com.cms.model.enums.StudentType;
import com.cms.model.enums.TermType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.EnquiryStatusHistoryRepository;
import com.cms.repository.TermBillingScheduleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class EnquiryPaymentService {

    private static final TypeReference<List<YearWiseFeeEntry>> YEAR_FEES_TYPE =
        new TypeReference<>() {};

    private static final TypeReference<List<TermWiseFeeEntry>> TERM_FEES_TYPE =
        new TypeReference<>() {};

    private static final Set<String> PAYMENT_BLOCKED_STATUSES = Set.of(
        "NOT_INTERESTED", "CANCELLED", "CLOSED", "ADMITTED", "CONVERTED"
    );

    private static final Set<EnquiryStatus> PAYMENT_ELIGIBLE_STATUSES = Set.of(
        EnquiryStatus.FEES_FINALIZED,
        EnquiryStatus.PARTIALLY_PAID,
        EnquiryStatus.FEES_PAID,
        EnquiryStatus.DOCUMENTS_SUBMITTED,
        EnquiryStatus.DOCUMENTS_VERIFIED
    );

    private static final Set<EnquiryStatus> PAYMENT_STATUS_FLOW_STATUSES = Set.of(
        EnquiryStatus.FEES_FINALIZED, EnquiryStatus.PARTIALLY_PAID, EnquiryStatus.FEES_PAID
    );

    private final EnquiryPaymentRepository enquiryPaymentRepository;
    private final EnquiryRepository enquiryRepository;
    private final EnquiryStatusHistoryRepository statusHistoryRepository;
    private final ObjectMapper objectMapper;
    private final UnifiedReceiptService unifiedReceiptService;
    private final AcademicYearRepository academicYearRepository;
    private final TermBillingScheduleRepository billingScheduleRepository;

    public EnquiryPaymentService(EnquiryPaymentRepository enquiryPaymentRepository,
                                  EnquiryRepository enquiryRepository,
                                  EnquiryStatusHistoryRepository statusHistoryRepository,
                                  ObjectMapper objectMapper,
                                  UnifiedReceiptService unifiedReceiptService,
                                  AcademicYearRepository academicYearRepository,
                                  TermBillingScheduleRepository billingScheduleRepository) {
        this.enquiryPaymentRepository = enquiryPaymentRepository;
        this.enquiryRepository = enquiryRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.objectMapper = objectMapper;
        this.unifiedReceiptService = unifiedReceiptService;
        this.academicYearRepository = academicYearRepository;
        this.billingScheduleRepository = billingScheduleRepository;
    }

    @Transactional
    public EnquiryPaymentResponse collectPayment(Long enquiryId, EnquiryPaymentRequest request, String collectedBy) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));

        validatePaymentEligibility(enquiry);

        BigDecimal totalPaidBefore = Optional.ofNullable(enquiryPaymentRepository.sumAmountPaidByEnquiryId(enquiryId))
            .orElse(BigDecimal.ZERO);
        BigDecimal outstandingBefore = enquiry.getFinalizedNetFee().subtract(totalPaidBefore).max(BigDecimal.ZERO);
        if (request.amountPaid().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Payment amount must be greater than zero");
        }
        if (outstandingBefore.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("No outstanding balance is available for this enquiry");
        }
        if (request.amountPaid().compareTo(outstandingBefore) > 0) {
            throw new IllegalStateException(
                "Payment amount cannot exceed outstanding balance: " + outstandingBefore
            );
        }

        String receiptNumber = unifiedReceiptService.generateReceiptNumber();

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

        BigDecimal totalPaid = totalPaidBefore.add(request.amountPaid());

        EnquiryStatus oldStatus = enquiry.getStatus();
        EnquiryStatus newStatus = resolvePostPaymentStatus(oldStatus, enquiry.getFinalizedNetFee(), totalPaid);

        if (newStatus != oldStatus) {
            enquiry.setStatus(newStatus);
            enquiryRepository.save(enquiry);

            statusHistoryRepository.save(new EnquiryStatusHistory(
                enquiry, oldStatus, newStatus, collectedBy, "Payment collected"
            ));
        }

        String feeCategory = enquiry.getStudentType() == StudentType.HOSTELER
            ? "TUITION_AND_HOSTEL" : "TUITION_ONLY";
        String towardsLabel = "TUITION_AND_HOSTEL".equals(feeCategory)
            ? "Tuition Fees And Hostel Fees" : "Tuition Fees";

        // Persist to the unified receipts table
        unifiedReceiptService.saveEnquiryReceipt(
            receiptNumber,
            enquiry.getId(), enquiry.getName(),
            enquiry.getCourse() != null ? enquiry.getCourse().getName()
                : enquiry.getProgram() != null ? enquiry.getProgram().getName() : null,
            request.amountPaid(), request.paymentDate(), request.paymentMode().name(),
            request.transactionReference(), request.remarks(),
            towardsLabel, collectedBy, feeCategory);

        return toResponse(saved, newStatus);
    }

    private void validatePaymentEligibility(Enquiry enquiry) {
        EnquiryStatus status = enquiry.getStatus();
        if (status == null
            || PAYMENT_BLOCKED_STATUSES.contains(status.name())
            || !PAYMENT_ELIGIBLE_STATUSES.contains(status)) {
            throw new IllegalStateException(
                "Payment cannot be collected for enquiry status: " + status
            );
        }
        if (enquiry.getFinalizedNetFee() == null || enquiry.getFinalizedNetFee().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Payment can be collected only after fees are finalized");
        }
    }

    private EnquiryStatus resolvePostPaymentStatus(EnquiryStatus currentStatus,
                                                    BigDecimal finalizedNetFee,
                                                    BigDecimal totalPaid) {
        if (!PAYMENT_STATUS_FLOW_STATUSES.contains(currentStatus)) {
            return currentStatus;
        }
        return totalPaid.compareTo(finalizedNetFee) >= 0
            ? EnquiryStatus.FEES_PAID
            : EnquiryStatus.PARTIALLY_PAID;
    }

    public BigDecimal getTotalAmountPaid(Long enquiryId) {
        return Optional.ofNullable(enquiryPaymentRepository.sumAmountPaidByEnquiryId(enquiryId))
            .orElse(BigDecimal.ZERO);
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

    public EnquiryYearWiseFeeStatusResponse getYearWiseFeeStatus(Long enquiryId) {
        Enquiry enquiry = enquiryRepository.findById(enquiryId)
            .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id: " + enquiryId));

        BigDecimal totalPaid = Optional.ofNullable(enquiryPaymentRepository.sumAmountPaidByEnquiryId(enquiryId))
            .orElse(BigDecimal.ZERO);

        // Resolve billing schedule dates from the current academic year
        AcademicYear currentAy = academicYearRepository.findByIsCurrentTrue().orElse(null);
        int baseStartYear = currentAy != null ? currentAy.getStartYear() : LocalDate.now().getYear();
        LocalDate baseOddDue  = currentAy == null ? null :
            billingScheduleRepository.findByAcademicYearIdAndTermType(currentAy.getId(), TermType.ODD)
                .map(TermBillingSchedule::getDueDate).orElse(null);
        LocalDate baseEvenDue = currentAy == null ? null :
            billingScheduleRepository.findByAcademicYearIdAndTermType(currentAy.getId(), TermType.EVEN)
                .map(TermBillingSchedule::getDueDate).orElse(null);

        // Kept only as last-resort fallback when no AY billing is configured at all
        LocalDate baseDate = enquiry.getFinalizedAt() != null
            ? enquiry.getFinalizedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            : LocalDate.now();

        // --- Year-wise breakdown (backward-compatible) ---
        List<YearWiseFeeEntry> yearEntries = parseYearWiseFees(enquiry.getYearWiseFees());
        yearEntries.sort(Comparator.comparingInt(YearWiseFeeEntry::yearNumber));

        BigDecimal yearTotalFee = yearEntries.stream()
            .map(YearWiseFeeEntry::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal yearRemaining = totalPaid;
        List<YearFeeStatus> yearBreakdown = new ArrayList<>();
        for (YearWiseFeeEntry entry : yearEntries) {
            BigDecimal allocated = entry.amount();
            BigDecimal paid = yearRemaining.min(allocated);
            BigDecimal outstanding = allocated.subtract(paid);
            yearBreakdown.add(new YearFeeStatus(entry.yearNumber(), allocated, paid, outstanding));
            yearRemaining = yearRemaining.subtract(paid);
        }

        // --- Term-wise breakdown (primary when available) ---
        List<TermWiseFeeEntry> termEntries = parseTermWiseFees(enquiry.getSemesterWiseFees());
        termEntries.sort(Comparator.comparingInt(TermWiseFeeEntry::termNumber));

        BigDecimal totalFee;
        BigDecimal termRemaining = totalPaid;
        List<InstallmentFeeStatus> installmentBreakdown = new ArrayList<>();

        if (!termEntries.isEmpty()) {
            totalFee = termEntries.stream()
                .map(TermWiseFeeEntry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            for (TermWiseFeeEntry entry : termEntries) {
                BigDecimal allocated = entry.amount();
                BigDecimal paid = termRemaining.min(allocated);
                BigDecimal outstanding = allocated.subtract(paid);
                int yearNumber = (entry.termNumber() + 1) / 2;
                int semSeq = ((entry.termNumber() - 1) % 2) + 1;
                LocalDate dueDate = dueForYear(yearNumber, semSeq, baseStartYear,
                    baseOddDue, baseEvenDue, baseDate);
                installmentBreakdown.add(new InstallmentFeeStatus(
                    entry.termNumber(), entry.installmentLabel(), allocated, paid, outstanding, dueDate));
                termRemaining = termRemaining.subtract(paid);
            }
        } else if (!yearEntries.isEmpty()) {
            // No explicit term data — auto-split each year into 2 installments
            totalFee = yearTotalFee;
            BigDecimal semRem = totalPaid;

            for (YearWiseFeeEntry entry : yearEntries) {
                BigDecimal sem1Amount = entry.amount().divide(BigDecimal.TWO, 2, RoundingMode.FLOOR);
                BigDecimal sem2Amount = entry.amount().subtract(sem1Amount);

                int sem1Seq = (entry.yearNumber() - 1) * 2 + 1;
                int sem2Seq = sem1Seq + 1;
                String sem1Label = "Year " + entry.yearNumber() + " - " + installmentOrdinalLabel(sem1Seq);
                String sem2Label = "Year " + entry.yearNumber() + " - " + installmentOrdinalLabel(sem2Seq);

                LocalDate sem1Due = dueForYear(entry.yearNumber(), 1, baseStartYear,
                    baseOddDue, baseEvenDue, baseDate);
                LocalDate sem2Due = dueForYear(entry.yearNumber(), 2, baseStartYear,
                    baseOddDue, baseEvenDue, baseDate);

                BigDecimal paid1 = semRem.min(sem1Amount);
                installmentBreakdown.add(new InstallmentFeeStatus(
                    sem1Seq, sem1Label, sem1Amount, paid1, sem1Amount.subtract(paid1), sem1Due));
                semRem = semRem.subtract(paid1).max(BigDecimal.ZERO);

                BigDecimal paid2 = semRem.min(sem2Amount);
                installmentBreakdown.add(new InstallmentFeeStatus(
                    sem2Seq, sem2Label, sem2Amount, paid2, sem2Amount.subtract(paid2), sem2Due));
                semRem = semRem.subtract(paid2).max(BigDecimal.ZERO);
            }
        } else {
            // No year or term breakdown configured — fall back to finalizedNetFee
            BigDecimal fallback = enquiry.getFinalizedNetFee() != null
                ? enquiry.getFinalizedNetFee()
                : BigDecimal.ZERO;
            totalFee = fallback.compareTo(BigDecimal.ZERO) > 0 ? fallback : yearTotalFee;
            if (totalFee.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal paid = totalPaid.min(totalFee);
                installmentBreakdown.add(new InstallmentFeeStatus(
                    1, "Full Program Fee", totalFee, paid, totalFee.subtract(paid), baseDate));
            }
        }

        BigDecimal totalOutstanding = totalFee.subtract(totalPaid.min(totalFee));
        return new EnquiryYearWiseFeeStatusResponse(
            enquiryId, totalFee, totalPaid, totalOutstanding, yearBreakdown, installmentBreakdown);
    }

    private List<YearWiseFeeEntry> parseYearWiseFees(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, YEAR_FEES_TYPE);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<TermWiseFeeEntry> parseTermWiseFees(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, TERM_FEES_TYPE);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private EnquiryPaymentResponse toResponse(EnquiryPayment payment, EnquiryStatus newStatus) {
        String feeCategory = payment.getEnquiry().getStudentType() == StudentType.HOSTELER
            ? "TUITION_AND_HOSTEL" : "TUITION_ONLY";
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
            feeCategory,
            newStatus != null ? newStatus.name() : null,
            payment.getCreatedAt()
        );
    }

    record YearWiseFeeEntry(int yearNumber, BigDecimal amount) {}

    record TermWiseFeeEntry(int termNumber, String installmentLabel, BigDecimal amount) {}

    private static final String[] ORDINALS = {
        "First", "Second", "Third", "Fourth", "Fifth", "Sixth",
        "Seventh", "Eighth", "Ninth", "Tenth", "Eleventh", "Twelfth"
    };

    private static String installmentOrdinalLabel(int globalSeq) {
        if (globalSeq >= 1 && globalSeq <= ORDINALS.length) {
            return ORDINALS[globalSeq - 1] + " Installment";
        }
        return "Installment " + globalSeq;
    }

    /** Returns the due date for a given year/semester using the AY billing schedule.
     *  semSeq 1 = ODD term, 2 = EVEN term. */
    private LocalDate dueForYear(int yearNumber, int semSeq, int baseStartYear,
                                  LocalDate baseOddDue, LocalDate baseEvenDue,
                                  LocalDate legacyFallback) {
        if (baseOddDue == null || baseEvenDue == null) {
            // No billing configured — fall back to finalization-date arithmetic
            LocalDate d = legacyFallback.plusMonths((long) (yearNumber - 1) * 12);
            return semSeq == 2 ? d.plusMonths(6) : d;
        }

        int targetStartYear = baseStartYear + (yearNumber - 1);
        TermType termType = semSeq == 2 ? TermType.EVEN : TermType.ODD;

        // Prefer the target academic year's own billing schedule if it exists
        Optional<AcademicYear> targetAyOpt = academicYearRepository
            .findByNameStartingWith(String.valueOf(targetStartYear));
        if (targetAyOpt.isPresent()) {
            Optional<LocalDate> billing = billingScheduleRepository
                .findByAcademicYearIdAndTermType(targetAyOpt.get().getId(), termType)
                .map(TermBillingSchedule::getDueDate);
            if (billing.isPresent()) return billing.get();
        }

        // Fallback: shift the base AY's billing date forward by the year delta
        LocalDate baseDue = semSeq == 2 ? baseEvenDue : baseOddDue;
        return baseDue.withYear(baseDue.getYear() + (targetStartYear - baseStartYear));
    }
}
