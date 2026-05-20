package com.cms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.EnquiryPaymentRequest;
import com.cms.dto.EnquiryPaymentResponse;
import com.cms.dto.EnquiryYearWiseFeeStatusResponse;
import com.cms.dto.EnquiryYearWiseFeeStatusResponse.InstallmentFeeStatus;
import com.cms.dto.EnquiryYearWiseFeeStatusResponse.YearFeeStatus;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Enquiry;
import com.cms.model.EnquiryPayment;
import com.cms.model.EnquiryStatusHistory;
import com.cms.model.enums.EnquiryStatus;
import com.cms.model.enums.StudentType;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.EnquiryStatusHistoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class EnquiryPaymentService {

    private static final TypeReference<List<YearWiseFeeEntry>> YEAR_FEES_TYPE =
        new TypeReference<>() {};

    private static final TypeReference<List<TermWiseFeeEntry>> TERM_FEES_TYPE =
        new TypeReference<>() {};

    private final EnquiryPaymentRepository enquiryPaymentRepository;
    private final EnquiryRepository enquiryRepository;
    private final EnquiryStatusHistoryRepository statusHistoryRepository;
    private final ObjectMapper objectMapper;
    private final UnifiedReceiptService unifiedReceiptService;

    public EnquiryPaymentService(EnquiryPaymentRepository enquiryPaymentRepository,
                                  EnquiryRepository enquiryRepository,
                                  EnquiryStatusHistoryRepository statusHistoryRepository,
                                  ObjectMapper objectMapper,
                                  UnifiedReceiptService unifiedReceiptService) {
        this.enquiryPaymentRepository = enquiryPaymentRepository;
        this.enquiryRepository = enquiryRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.objectMapper = objectMapper;
        this.unifiedReceiptService = unifiedReceiptService;
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

        // Base date for computing due dates: finalization date or today
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
                LocalDate dueDate = baseDate
                    .plusMonths((long) (yearNumber - 1) * 12)
                    .plusMonths(semSeq == 2 ? 6 : 0);
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

                LocalDate sem1Due = baseDate.plusMonths((long) (entry.yearNumber() - 1) * 12);
                LocalDate sem2Due = sem1Due.plusMonths(6);

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
}
