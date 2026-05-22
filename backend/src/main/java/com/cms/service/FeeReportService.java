package com.cms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.FeeDemandDto;
import com.cms.dto.FeeCollectionSummaryDto;
import com.cms.dto.StudentFeeLedgerDto;
import com.cms.dto.TermFeePaymentDto;
import com.cms.dto.YearFeeFromEnquiry;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.EnquiryPayment;
import com.cms.model.FeeDemand;
import com.cms.model.FeeInstallment;
import com.cms.model.SemesterFee;
import com.cms.model.Student;
import com.cms.model.TermFeePayment;
import com.cms.model.enums.DemandStatus;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FeeDemandRepository;
import com.cms.repository.FeeInstallmentRepository;
import com.cms.repository.SemesterFeeRepository;
import com.cms.repository.StudentFeeAllocationRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.TermFeePaymentRepository;

@Service
@Transactional(readOnly = true)
public class FeeReportService {

    private final FeeDemandRepository feeDemandRepository;
    private final TermFeePaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final FeeDemandService feeDemandService;
    private final StudentFeeAllocationRepository allocationRepository;
    private final SemesterFeeRepository semesterFeeRepository;
    private final FeeInstallmentRepository installmentRepository;
    private final EnquiryRepository enquiryRepository;
    private final EnquiryPaymentRepository enquiryPaymentRepository;
    private final FeeFinalizationService feeFinalizationService;

    public FeeReportService(FeeDemandRepository feeDemandRepository,
                             TermFeePaymentRepository paymentRepository,
                             StudentRepository studentRepository,
                             FeeDemandService feeDemandService,
                             StudentFeeAllocationRepository allocationRepository,
                             SemesterFeeRepository semesterFeeRepository,
                             FeeInstallmentRepository installmentRepository,
                             EnquiryRepository enquiryRepository,
                             EnquiryPaymentRepository enquiryPaymentRepository,
                             FeeFinalizationService feeFinalizationService) {
        this.feeDemandRepository = feeDemandRepository;
        this.paymentRepository = paymentRepository;
        this.studentRepository = studentRepository;
        this.feeDemandService = feeDemandService;
        this.allocationRepository = allocationRepository;
        this.semesterFeeRepository = semesterFeeRepository;
        this.installmentRepository = installmentRepository;
        this.enquiryRepository = enquiryRepository;
        this.enquiryPaymentRepository = enquiryPaymentRepository;
        this.feeFinalizationService = feeFinalizationService;
    }

    public List<FeeDemandDto> getOutstandingDemands(Long termInstanceId) {
        return feeDemandService.getOutstandingDemands(termInstanceId);
    }

    public List<FeeCollectionSummaryDto> getCollectionSummary(Long termInstanceId) {
        List<FeeDemand> demands = feeDemandRepository.findByTermInstanceId(termInstanceId);

        // Group by program
        Map<Long, ProgramAccumulator> accMap = new LinkedHashMap<>();
        for (FeeDemand demand : demands) {
            Long programId = demand.getStudentTermEnrollment().getCohort().getProgram().getId();
            ProgramAccumulator acc = accMap.computeIfAbsent(programId, id -> new ProgramAccumulator(
                demand.getStudentTermEnrollment().getCohort().getProgram().getName(),
                demand.getStudentTermEnrollment().getCohort().getProgram().getCode()
            ));
            acc.add(demand);
        }

        return accMap.values().stream().map(ProgramAccumulator::toDto).toList();
    }

    public List<TermFeePaymentDto> getLateFeeCollection(Long termInstanceId) {
        List<FeeDemand> demands = feeDemandRepository.findByTermInstanceId(termInstanceId);
        List<TermFeePaymentDto> result = new ArrayList<>();
        for (FeeDemand demand : demands) {
            List<TermFeePayment> payments = paymentRepository.findByFeeDemandId(demand.getId());
            for (TermFeePayment p : payments) {
                if (p.getLateFeeApplied() != null
                        && p.getLateFeeApplied().compareTo(BigDecimal.ZERO) > 0) {
                    result.add(toPaymentDto(p));
                }
            }
        }
        return result;
    }

    public StudentFeeLedgerDto getStudentLedger(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        Optional<StudentFeeLedgerDto> allocationLedger = buildAllocationLedger(student);
        if (allocationLedger.isPresent()) {
            return allocationLedger.get();
        }

        List<FeeDemand> demands = feeDemandRepository.findByStudentTermEnrollmentStudentId(studentId);
        if (!demands.isEmpty()) {
            // Old FeeDemand-based flow — keep existing behaviour
            List<StudentFeeLedgerDto.LedgerEntry> entries = new ArrayList<>();
            for (FeeDemand demand : demands) {
                String termLabel = demand.getTermInstance().getAcademicYear().getName()
                    + " " + demand.getTermInstance().getTermType();
                List<TermFeePayment> payments = paymentRepository.findByFeeDemandId(demand.getId());
                List<TermFeePaymentDto> paymentDtos = payments.stream().map(this::toPaymentDto).toList();
                entries.add(new StudentFeeLedgerDto.LedgerEntry(
                    demand.getId(), termLabel,
                    demand.getTotalAmount(), demand.getPaidAmount(), demand.getOutstandingAmount(),
                    demand.getDueDate(), demand.getStatus(), paymentDtos
                ));
            }
            return new StudentFeeLedgerDto(student.getId(), student.getFullName(), entries);
        }

        return buildEnquiryPreviewLedger(student)
            .orElse(new StudentFeeLedgerDto(student.getId(), student.getFullName(), List.of()));
    }

    private Optional<StudentFeeLedgerDto> buildAllocationLedger(Student student) {
        // New flow: StudentFeeAllocation is the single source of truth whenever present.
        // Enquiry payments are distributed across installments in order.
        Long studentId = student.getId();
        return allocationRepository.findByStudentId(studentId)
            .flatMap(allocation -> {
                List<SemesterFee> semesterFees = semesterFeeRepository
                    .findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(allocation.getId());
                if (semesterFees.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(new StudentFeeLedgerDto(
                    student.getId(), student.getFullName(), buildEntriesFromSemesterFees(student, semesterFees)
                ));
            });
    }

    private Optional<StudentFeeLedgerDto> buildEnquiryPreviewLedger(Student student) {
        List<YearFeeFromEnquiry> yearFees;
        try {
            yearFees = feeFinalizationService.getEnquiryYearFees(student.getId());
        } catch (ResourceNotFoundException ex) {
            return Optional.empty();
        }
        if (yearFees.isEmpty()) {
            return Optional.empty();
        }

        List<SemesterFeePreview> previews = new ArrayList<>();
        for (YearFeeFromEnquiry yearFee : yearFees) {
            BigDecimal firstAmount = yearFee.amount().divide(BigDecimal.TWO, 2, RoundingMode.FLOOR);
            BigDecimal secondAmount = yearFee.amount().subtract(firstAmount);
            int firstSequence = (yearFee.yearNumber() - 1) * 2 + 1;
            int secondSequence = firstSequence + 1;
            previews.add(new SemesterFeePreview(
                -1L * firstSequence,
                "Year " + yearFee.yearNumber() + " - " + installmentOrdinalLabel(firstSequence),
                firstAmount,
                yearFee.dueDate()
            ));
            previews.add(new SemesterFeePreview(
                -1L * secondSequence,
                "Year " + yearFee.yearNumber() + " - " + installmentOrdinalLabel(secondSequence),
                secondAmount,
                yearFee.dueDate().plusMonths(6)
            ));
        }

        BigDecimal remainingEnquiryCredit = enquiryRepository
            .findByConvertedStudentId(student.getId())
            .map(e -> enquiryPaymentRepository.sumAmountPaidByEnquiryId(e.getId()))
            .orElse(BigDecimal.ZERO);

        List<EnquiryPayment> enquiryPayments = enquiryRepository
            .findByConvertedStudentId(student.getId())
            .map(e -> enquiryPaymentRepository.findByEnquiryIdOrderByPaymentDateDesc(e.getId()))
            .orElse(List.of());

        List<StudentFeeLedgerDto.LedgerEntry> entries = new ArrayList<>();
        for (SemesterFeePreview preview : previews) {
            BigDecimal creditForThis = remainingEnquiryCredit.min(preview.amount());
            remainingEnquiryCredit = remainingEnquiryCredit.subtract(creditForThis);
            BigDecimal outstanding = preview.amount().subtract(creditForThis).max(BigDecimal.ZERO);

            DemandStatus status;
            if (outstanding.compareTo(BigDecimal.ZERO) <= 0) status = DemandStatus.PAID;
            else if (creditForThis.compareTo(BigDecimal.ZERO) > 0) status = DemandStatus.PARTIAL;
            else status = DemandStatus.UNPAID;

            List<TermFeePaymentDto> paymentDtos = creditForThis.compareTo(BigDecimal.ZERO) > 0
                ? enquiryPayments.stream().map(ep -> enquiryPaymentToDto(ep, status)).toList()
                : List.of();

            entries.add(new StudentFeeLedgerDto.LedgerEntry(
                preview.id(), preview.label(), preview.amount(), creditForThis, outstanding,
                preview.dueDate(), status, paymentDtos
            ));
        }

        return Optional.of(new StudentFeeLedgerDto(student.getId(), student.getFullName(), entries));
    }

    private List<StudentFeeLedgerDto.LedgerEntry> buildEntriesFromSemesterFees(
            Student student, List<SemesterFee> semesterFees) {
        Long studentId = student.getId();
        BigDecimal remainingEnquiryCredit = enquiryRepository
            .findByConvertedStudentId(studentId)
            .map(e -> enquiryPaymentRepository.sumAmountPaidByEnquiryId(e.getId()))
            .orElse(BigDecimal.ZERO);

        List<EnquiryPayment> enquiryPayments = enquiryRepository
            .findByConvertedStudentId(studentId)
            .map(e -> enquiryPaymentRepository.findByEnquiryIdOrderByPaymentDateDesc(e.getId()))
            .orElse(List.of());

        List<StudentFeeLedgerDto.LedgerEntry> entries = new ArrayList<>();
        for (SemesterFee sf : semesterFees) {
            BigDecimal feeInstallmentPaid = installmentRepository.sumAmountPaidBySemesterFeeId(sf.getId());
            BigDecimal maxCredit = sf.getAmount().subtract(feeInstallmentPaid).max(BigDecimal.ZERO);
            BigDecimal creditForThis = remainingEnquiryCredit.min(maxCredit);
            remainingEnquiryCredit = remainingEnquiryCredit.subtract(creditForThis);
            BigDecimal totalPaid = feeInstallmentPaid.add(creditForThis);
            BigDecimal outstanding = sf.getAmount().subtract(totalPaid).max(BigDecimal.ZERO);

            DemandStatus status;
            if (outstanding.compareTo(BigDecimal.ZERO) <= 0) status = DemandStatus.PAID;
            else if (totalPaid.compareTo(BigDecimal.ZERO) > 0)  status = DemandStatus.PARTIAL;
            else                                               status = DemandStatus.UNPAID;

            // Build payment rows from FeeInstallment records for this semester
            List<FeeInstallment> installments = installmentRepository.findBySemesterFeeId(sf.getId());
            List<TermFeePaymentDto> paymentDtos = installments.stream()
                .map(fi -> installmentToPaymentDto(fi, student.getFullName(), status))
                .collect(Collectors.toCollection(ArrayList::new));

            // Prepend enquiry payment receipts that were credited to this installment
            if (creditForThis.compareTo(BigDecimal.ZERO) > 0) {
                List<TermFeePaymentDto> enquiryPaymentDtos = enquiryPayments.stream()
                    .map(ep -> enquiryPaymentToDto(ep, status))
                    .toList();
                paymentDtos.addAll(0, enquiryPaymentDtos);
            }

            entries.add(new StudentFeeLedgerDto.LedgerEntry(
                sf.getId(), sf.getSemesterLabel(),
                sf.getAmount(), totalPaid, outstanding,
                sf.getDueDate(), status, paymentDtos
            ));
        }
        return entries;
    }

    private static final String[] ORDINALS = {
        "First", "Second", "Third", "Fourth", "Fifth", "Sixth",
        "Seventh", "Eighth", "Ninth", "Tenth", "Eleventh", "Twelfth"
    };

    private static String installmentOrdinalLabel(int sequence) {
        if (sequence >= 1 && sequence <= ORDINALS.length) {
            return ORDINALS[sequence - 1] + " Installment";
        }
        return "Installment " + sequence;
    }

    private record SemesterFeePreview(Long id, String label, BigDecimal amount, java.time.LocalDate dueDate) {}

    private TermFeePaymentDto installmentToPaymentDto(FeeInstallment fi, String studentName, DemandStatus status) {
        return new TermFeePaymentDto(
            fi.getId(), null, studentName,
            fi.getPaymentDate(), fi.getAmountPaid(), BigDecimal.ZERO, fi.getAmountPaid(),
            fi.getPaymentMode(), fi.getReceiptNumber(), fi.getTransactionReference(),
            status, fi.getCreatedAt(), fi.getCreatedAt()
        );
    }

    private TermFeePaymentDto enquiryPaymentToDto(EnquiryPayment ep, DemandStatus status) {
        return new TermFeePaymentDto(
            ep.getId(), null, null,
            ep.getPaymentDate(), ep.getAmountPaid(), BigDecimal.ZERO, ep.getAmountPaid(),
            ep.getPaymentMode(), ep.getReceiptNumber(), ep.getTransactionReference(),
            status, ep.getCreatedAt(), ep.getCreatedAt()
        );
    }

    private TermFeePaymentDto toPaymentDto(TermFeePayment p) {
        FeeDemand demand = p.getFeeDemand();
        String studentName = demand.getStudentTermEnrollment().getStudent().getFullName();
        return new TermFeePaymentDto(
            p.getId(),
            demand.getId(),
            studentName,
            p.getPaymentDate(),
            p.getAmountPaid(),
            p.getLateFeeApplied(),
            p.getTotalCollected(),
            p.getPaymentMode(),
            p.getReceiptNumber(),
            p.getTransactionReference(),
            p.getRemarks(),
            demand.getStatus(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }

    /** Internal accumulator for summary grouping. */
    private static class ProgramAccumulator {
        final String programName;
        final String programCode;
        long totalDemands = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal collectedAmount = BigDecimal.ZERO;
        long paidCount = 0;
        long partialCount = 0;
        long unpaidCount = 0;

        ProgramAccumulator(String programName, String programCode) {
            this.programName = programName;
            this.programCode = programCode;
        }

        void add(FeeDemand demand) {
            totalDemands++;
            totalAmount = totalAmount.add(demand.getTotalAmount());
            collectedAmount = collectedAmount.add(demand.getPaidAmount());
            if (demand.getStatus() == DemandStatus.PAID) {
                paidCount++;
            } else if (demand.getStatus() == DemandStatus.PARTIAL) {
                partialCount++;
            } else if (demand.getStatus() == DemandStatus.UNPAID) {
                unpaidCount++;
            }
        }

        FeeCollectionSummaryDto toDto() {
            BigDecimal outstanding = totalAmount.subtract(collectedAmount).max(BigDecimal.ZERO);
            return new FeeCollectionSummaryDto(
                programName, programCode, totalDemands,
                totalAmount, collectedAmount, outstanding,
                paidCount, partialCount, unpaidCount
            );
        }
    }
}
