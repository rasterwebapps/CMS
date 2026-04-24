package com.cms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.EnquiryPaymentRequest;
import com.cms.dto.EnquiryPaymentResponse;
import com.cms.dto.EnquiryYearWiseFeeStatusResponse;
import com.cms.dto.EnquiryYearWiseFeeStatusResponse.SemesterFeeStatus;
import com.cms.dto.EnquiryYearWiseFeeStatusResponse.YearFeeStatus;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Enquiry;
import com.cms.model.EnquiryPayment;
import com.cms.model.EnquiryStatusHistory;
import com.cms.model.enums.EnquiryStatus;
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

    private static final TypeReference<List<SemesterWiseFeeEntry>> SEMESTER_FEES_TYPE =
        new TypeReference<>() {};

    private final EnquiryPaymentRepository enquiryPaymentRepository;
    private final EnquiryRepository enquiryRepository;
    private final EnquiryStatusHistoryRepository statusHistoryRepository;
    private final ObjectMapper objectMapper;

    public EnquiryPaymentService(EnquiryPaymentRepository enquiryPaymentRepository,
                                  EnquiryRepository enquiryRepository,
                                  EnquiryStatusHistoryRepository statusHistoryRepository,
                                  ObjectMapper objectMapper) {
        this.enquiryPaymentRepository = enquiryPaymentRepository;
        this.enquiryRepository = enquiryRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.objectMapper = objectMapper;
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

        // --- Semester-wise breakdown (primary when available) ---
        List<SemesterWiseFeeEntry> semesterEntries = parseSemesterWiseFees(enquiry.getSemesterWiseFees());
        semesterEntries.sort(Comparator.comparingInt(SemesterWiseFeeEntry::semesterNumber));

        BigDecimal totalFee;
        BigDecimal semesterRemaining = totalPaid;
        List<SemesterFeeStatus> semesterBreakdown = new ArrayList<>();

        if (!semesterEntries.isEmpty()) {
            totalFee = semesterEntries.stream()
                .map(SemesterWiseFeeEntry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            for (SemesterWiseFeeEntry entry : semesterEntries) {
                BigDecimal allocated = entry.amount();
                BigDecimal paid = semesterRemaining.min(allocated);
                BigDecimal outstanding = allocated.subtract(paid);
                semesterBreakdown.add(new SemesterFeeStatus(
                    entry.semesterNumber(), entry.semesterLabel(), allocated, paid, outstanding));
                semesterRemaining = semesterRemaining.subtract(paid);
            }
        } else {
            totalFee = yearTotalFee;
        }

        BigDecimal totalOutstanding = totalFee.subtract(totalPaid.min(totalFee));
        return new EnquiryYearWiseFeeStatusResponse(
            enquiryId, totalFee, totalPaid, totalOutstanding, yearBreakdown, semesterBreakdown);
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

    private List<SemesterWiseFeeEntry> parseSemesterWiseFees(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, SEMESTER_FEES_TYPE);
        } catch (Exception e) {
            return new ArrayList<>();
        }
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

    record YearWiseFeeEntry(int yearNumber, BigDecimal amount) {}

    record SemesterWiseFeeEntry(int semesterNumber, String semesterLabel, BigDecimal amount) {}
}
