package com.cms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.StudentFeeAllocationRequest;
import com.cms.dto.StudentFeeAllocationResponse;
import com.cms.dto.YearFeeFromEnquiry;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Enquiry;
import com.cms.model.Penalty;
import com.cms.model.SemesterFee;
import com.cms.model.Student;
import com.cms.model.StudentFeeAllocation;
import com.cms.model.StudentScholarship;
import com.cms.model.TermBillingSchedule;
import com.cms.model.enums.FeeAllocationStatus;
import com.cms.model.enums.StudentType;
import com.cms.model.enums.TermType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FeeInstallmentRepository;
import com.cms.repository.PenaltyRepository;
import com.cms.repository.SemesterFeeRepository;
import com.cms.repository.StudentFeeAllocationRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.TermBillingScheduleRepository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class FeeFinalizationService {

    private final StudentFeeAllocationRepository allocationRepository;
    private final SemesterFeeRepository semesterFeeRepository;
    private final FeeInstallmentRepository installmentRepository;
    private final PenaltyRepository penaltyRepository;
    private final StudentRepository studentRepository;
    private final EnquiryRepository enquiryRepository;
    private final EnquiryPaymentRepository enquiryPaymentRepository;
    private final StudentScholarshipService studentScholarshipService;
    private final TermBillingScheduleRepository billingScheduleRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TermInstanceService termInstanceService;
    private final ObjectMapper objectMapper;

    public FeeFinalizationService(StudentFeeAllocationRepository allocationRepository,
                                   SemesterFeeRepository semesterFeeRepository,
                                   FeeInstallmentRepository installmentRepository,
                                   PenaltyRepository penaltyRepository,
                                   StudentRepository studentRepository,
                                   EnquiryRepository enquiryRepository,
                                   EnquiryPaymentRepository enquiryPaymentRepository,
                                   StudentScholarshipService studentScholarshipService,
                                   TermBillingScheduleRepository billingScheduleRepository,
                                   AcademicYearRepository academicYearRepository,
                                   TermInstanceService termInstanceService,
                                   ObjectMapper objectMapper) {
        this.allocationRepository = allocationRepository;
        this.semesterFeeRepository = semesterFeeRepository;
        this.installmentRepository = installmentRepository;
        this.penaltyRepository = penaltyRepository;
        this.studentRepository = studentRepository;
        this.enquiryRepository = enquiryRepository;
        this.enquiryPaymentRepository = enquiryPaymentRepository;
        this.studentScholarshipService = studentScholarshipService;
        this.billingScheduleRepository = billingScheduleRepository;
        this.academicYearRepository = academicYearRepository;
        this.termInstanceService = termInstanceService;
        this.objectMapper = objectMapper;
    }

    /** Returns year-wise fees from the enquiry linked to this student (pre-fill data). */
    public List<YearFeeFromEnquiry> getEnquiryYearFees(Long studentId) {
        Enquiry enquiry = enquiryRepository.findByConvertedStudentId(studentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No linked enquiry found for student: " + studentId));
        return parseYearWiseFeesJson(enquiry.getYearWiseFees());
    }

    private List<YearFeeFromEnquiry> parseYearWiseFeesJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            List<Map<String, Object>> parsed = objectMapper.readValue(json, new TypeReference<>() {});
            LocalDate baseDate = LocalDate.now().withDayOfMonth(1);
            return parsed.stream()
                .map(item -> {
                    int year   = ((Number) item.get("yearNumber")).intValue();
                    BigDecimal amount = new BigDecimal(item.get("amount").toString());
                    LocalDate dueDate = baseDate.plusMonths((long) (year - 1) * 12);
                    return new YearFeeFromEnquiry(year, amount, dueDate);
                })
                .sorted(Comparator.comparingInt(YearFeeFromEnquiry::yearNumber))
                .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Returns true if a fee allocation already exists for this student. */
    public boolean allocationExists(Long studentId) {
        return allocationRepository.existsByStudentId(studentId);
    }

    /**
     * Closes the gap between admission (student record created) and fee collection becoming
     * possible on the student side: called right at conversion time so the allocation exists
     * immediately, instead of waiting for someone to first open the student's Fee Detail page
     * (the previous, implicit trigger). A no-op when the enquiry's fees haven't been finalized
     * yet (no year-wise fee schedule to build from) — the lazy fallback on Fee Detail still
     * covers that ordering. Swallows failures rather than risking the admission transaction:
     * this is a convenience step, not a precondition for admitting a student.
     */
    @Transactional
    public void autoFinalizeFromEnquiry(Student student, Enquiry enquiry, String performedBy) {
        if (allocationRepository.existsByStudentId(student.getId())) {
            return;
        }
        List<YearFeeFromEnquiry> yearFees = parseYearWiseFeesJson(enquiry.getYearWiseFees());
        if (yearFees.isEmpty()) {
            return;
        }
        try {
            BigDecimal totalFee = yearFees.stream()
                .map(YearFeeFromEnquiry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            StudentFeeAllocationRequest request = new StudentFeeAllocationRequest(
                student.getId(), totalFee, null, null, null,
                yearFees.stream()
                    .map(f -> new StudentFeeAllocationRequest.YearFee(f.yearNumber(), f.amount()))
                    .toList()
            );
            finalize(request, performedBy);
        } catch (Exception e) {
            // Fall back to the lazy auto-init on the student's Fee Detail page.
        }
    }

    @Transactional
    public StudentFeeAllocationResponse finalize(StudentFeeAllocationRequest request, String adminUsername) {
        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.studentId()));

        if (allocationRepository.existsByStudentId(request.studentId())) {
            throw new IllegalStateException("Fee allocation already exists for student: " + student.getRollNumber());
        }

        BigDecimal manualDiscount = request.discountAmount() != null ? request.discountAmount() : BigDecimal.ZERO;
        StudentScholarship approvedScholarship = studentScholarshipService
            .findApprovedForStudentInCurrentYear(student.getId())
            .orElse(null);
        BigDecimal scholarshipDiscount = approvedScholarship != null
            ? approvedScholarship.getApprovedAmount()
            : BigDecimal.ZERO;
        BigDecimal discount = manualDiscount.add(scholarshipDiscount);
        BigDecimal commission = request.agentCommission() != null ? request.agentCommission() : BigDecimal.ZERO;
        BigDecimal netFee = request.totalFee().subtract(discount);
        if (netFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total discount cannot exceed total fee");
        }

        String discountReason = combineDiscountReasons(request.discountReason(), approvedScholarship);

        StudentFeeAllocation allocation = new StudentFeeAllocation(
            student, student.getProgram(), request.totalFee(),
            discount, discountReason, commission, netFee,
            FeeAllocationStatus.FINALIZED
        );
        if (approvedScholarship != null) {
            allocation.setScholarshipApplication(approvedScholarship);
            allocation.setScholarshipDiscountAmount(scholarshipDiscount);
            allocation.setScholarshipDiscountReason(approvedScholarship.getScholarshipType().getName());
        }
        allocation.setFinalizedAt(Instant.now());
        allocation.setFinalizedBy(adminUsername);

        // Detect hostel from the linked enquiry if available
        boolean hasHostelFee = enquiryRepository.findByConvertedStudentId(request.studentId())
            .map(e -> e.getStudentType() == StudentType.HOSTELER)
            .orElse(false);
        allocation.setHasHostelFee(hasHostelFee);

        StudentFeeAllocation saved = allocationRepository.save(allocation);

        int joiningStartYear = student.getCohort() != null
            ? student.getCohort().getAdmissionAcademicYear().getStartYear()
            : LocalDate.now().getYear();

        // Load admission-year billing dates as fallback for years whose AY isn't created yet.
        // When there is no cohort, resolve the admission AY by start year directly.
        Long admissionAyId = student.getCohort() != null
            ? student.getCohort().getAdmissionAcademicYear().getId()
            : academicYearRepository
                .findByNameStartingWith(String.valueOf(joiningStartYear))
                .map(AcademicYear::getId)
                .orElse(null);
        LocalDate fallbackOdd = admissionAyId == null ? null :
            billingScheduleRepository.findByAcademicYearIdAndTermType(admissionAyId, TermType.ODD)
                .map(TermBillingSchedule::getDueDate).orElse(null);
        LocalDate fallbackEven = admissionAyId == null ? null :
            billingScheduleRepository.findByAcademicYearIdAndTermType(admissionAyId, TermType.EVEN)
                .map(TermBillingSchedule::getDueDate).orElse(null);

        List<SemesterFee> semesterFees = new ArrayList<>();
        for (StudentFeeAllocationRequest.YearFee yearFee : request.yearFees()) {
            BigDecimal sem1Amount = yearFee.amount().divide(BigDecimal.TWO, 0, RoundingMode.FLOOR);
            BigDecimal sem2Amount = yearFee.amount().subtract(sem1Amount);

            int targetStartYear = joiningStartYear + (yearFee.yearNumber() - 1);

            Optional<AcademicYear> yearNOpt = academicYearRepository
                .findByNameStartingWith(String.valueOf(targetStartYear));

            LocalDate oddDueDate;
            LocalDate evenDueDate;

            if (yearNOpt.isPresent()) {
                Long ayId = yearNOpt.get().getId();
                oddDueDate  = billingScheduleRepository.findByAcademicYearIdAndTermType(ayId, TermType.ODD)
                    .map(TermBillingSchedule::getDueDate)
                    .orElseGet(() -> shiftDueYear(fallbackOdd, joiningStartYear, targetStartYear));
                evenDueDate = billingScheduleRepository.findByAcademicYearIdAndTermType(ayId, TermType.EVEN)
                    .map(TermBillingSchedule::getDueDate)
                    .orElseGet(() -> shiftDueYear(fallbackEven, joiningStartYear, targetStartYear));
            } else {
                oddDueDate  = shiftDueYear(fallbackOdd,  joiningStartYear, targetStartYear);
                evenDueDate = shiftDueYear(fallbackEven, joiningStartYear, targetStartYear);
            }

            int globalSem1 = (yearFee.yearNumber() - 1) * 2 + 1;
            int globalSem2 = globalSem1 + 1;

            SemesterFee sf1 = new SemesterFee(
                saved, yearFee.yearNumber(),
                "Year " + yearFee.yearNumber() + " - " + installmentOrdinalLabel(globalSem1),
                sem1Amount, oddDueDate, 1
            );
            semesterFees.add(semesterFeeRepository.save(sf1));

            SemesterFee sf2 = new SemesterFee(
                saved, yearFee.yearNumber(),
                "Year " + yearFee.yearNumber() + " - " + installmentOrdinalLabel(globalSem2),
                sem2Amount, evenDueDate, 2
            );
            semesterFees.add(semesterFeeRepository.save(sf2));
        }

        return toResponse(saved, semesterFees);
    }

    public StudentFeeAllocationResponse getByStudentId(Long studentId) {
        StudentFeeAllocation allocation = allocationRepository.findByStudentId(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Fee allocation not found for student id: " + studentId));

        List<SemesterFee> semesterFees = semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(allocation.getId());
        return toResponse(allocation, semesterFees);
    }

    public StudentFeeAllocationResponse getById(Long id) {
        StudentFeeAllocation allocation = allocationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fee allocation not found with id: " + id));

        List<SemesterFee> semesterFees = semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(allocation.getId());
        return toResponse(allocation, semesterFees);
    }

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

    private StudentFeeAllocationResponse toResponse(StudentFeeAllocation allocation, List<SemesterFee> semesterFees) {
        // Enquiry payments act as a pre-payment credit distributed across installments in order.
        BigDecimal remainingEnquiryCredit = enquiryRepository
            .findByConvertedStudentId(allocation.getStudent().getId())
            .map(e -> enquiryPaymentRepository.sumAmountPaidByEnquiryId(e.getId()))
            .orElse(BigDecimal.ZERO);

        int joiningStartYear = termInstanceService.resolveJoiningStartYear(allocation.getStudent());

        List<StudentFeeAllocationResponse.InstallmentFeeDetail> details = new ArrayList<>();
        for (SemesterFee sf : semesterFees) {
            BigDecimal feeInstallmentPaid = installmentRepository.sumAmountPaidBySemesterFeeId(sf.getId());

            BigDecimal maxCredit = sf.getAmount().subtract(feeInstallmentPaid).max(BigDecimal.ZERO);
            BigDecimal creditForThis = remainingEnquiryCredit.min(maxCredit);
            remainingEnquiryCredit = remainingEnquiryCredit.subtract(creditForThis);

            BigDecimal totalPaid = feeInstallmentPaid.add(creditForThis);
            BigDecimal pending = sf.getAmount().subtract(totalPaid).max(BigDecimal.ZERO);

            BigDecimal penaltyAmount = penaltyRepository.findBySemesterFeeId(sf.getId()).stream()
                .filter(p -> !p.getIsPaid())
                .map(Penalty::getTotalPenalty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            String paymentStatus;
            if (pending.compareTo(BigDecimal.ZERO) <= 0) {
                paymentStatus = "PAID";
            } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
                paymentStatus = "PARTIAL";
            } else {
                paymentStatus = "PENDING";
            }

            boolean collectibleNow = termInstanceService.isSemesterFeeCollectibleNow(
                joiningStartYear, sf.getYearNumber(), sf.getSemesterSequence());

            details.add(new StudentFeeAllocationResponse.InstallmentFeeDetail(
                sf.getId(), sf.getYearNumber(), sf.getSemesterSequence(), sf.getSemesterLabel(),
                sf.getAmount(), sf.getDueDate(), totalPaid, pending, penaltyAmount, paymentStatus,
                collectibleNow
            ));
        }

        return new StudentFeeAllocationResponse(
            allocation.getId(),
            allocation.getStudent().getId(),
            allocation.getStudent().getFullName(),
            allocation.getStudent().getRollNumber(),
            allocation.getProgram().getId(),
            allocation.getProgram().getName(),
            allocation.getTotalFee(),
            allocation.getDiscountAmount(),
            allocation.getDiscountReason(),
            allocation.getScholarshipApplication() != null ? allocation.getScholarshipApplication().getId() : null,
            allocation.getScholarshipDiscountAmount(),
            allocation.getScholarshipDiscountReason(),
            allocation.getAgentCommission(),
            allocation.getNetFee(),
            allocation.getStatus().name(),
            allocation.getFinalizedAt(),
            allocation.getFinalizedBy(),
            details,
            allocation.getCreatedAt(),
            allocation.getUpdatedAt()
        );
    }

    /**
     * Shifts a billing due date from the admission year's billing to the target academic year.
     * Preserves the same month and day; only the year changes by the same delta as the AY.
     * Falls back to today if reference is null (should not happen in normal operation).
     */
    private LocalDate shiftDueYear(LocalDate reference, int fromStartYear, int toStartYear) {
        if (reference == null) return LocalDate.now();
        return reference.withYear(reference.getYear() + (toStartYear - fromStartYear));
    }

    private String combineDiscountReasons(String manualReason, StudentScholarship approvedScholarship) {
        String scholarshipReason = approvedScholarship != null
            ? approvedScholarship.getScholarshipType().getName()
            : null;
        boolean hasManual = manualReason != null && !manualReason.isBlank();
        boolean hasScholarship = scholarshipReason != null && !scholarshipReason.isBlank();
        if (hasManual && hasScholarship) {
            return manualReason.trim() + " + " + scholarshipReason;
        }
        if (hasScholarship) {
            return scholarshipReason;
        }
        return hasManual ? manualReason.trim() : null;
    }
}
