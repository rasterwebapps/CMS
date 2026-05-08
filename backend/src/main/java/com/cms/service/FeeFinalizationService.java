package com.cms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.StudentFeeAllocationRequest;
import com.cms.dto.StudentFeeAllocationResponse;
import com.cms.dto.YearFeeFromEnquiry;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Enquiry;
import com.cms.model.Penalty;
import com.cms.model.SemesterFee;
import com.cms.model.Student;
import com.cms.model.StudentFeeAllocation;
import com.cms.model.StudentScholarship;
import com.cms.model.enums.FeeAllocationStatus;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FeeInstallmentRepository;
import com.cms.repository.PenaltyRepository;
import com.cms.repository.SemesterFeeRepository;
import com.cms.repository.StudentFeeAllocationRepository;
import com.cms.repository.StudentRepository;

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
    private final StudentScholarshipService studentScholarshipService;
    private final ObjectMapper objectMapper;

    public FeeFinalizationService(StudentFeeAllocationRepository allocationRepository,
                                   SemesterFeeRepository semesterFeeRepository,
                                   FeeInstallmentRepository installmentRepository,
                                   PenaltyRepository penaltyRepository,
                                   StudentRepository studentRepository,
                                   EnquiryRepository enquiryRepository,
                                    StudentScholarshipService studentScholarshipService,
                                   ObjectMapper objectMapper) {
        this.allocationRepository = allocationRepository;
        this.semesterFeeRepository = semesterFeeRepository;
        this.installmentRepository = installmentRepository;
        this.penaltyRepository = penaltyRepository;
        this.studentRepository = studentRepository;
        this.enquiryRepository = enquiryRepository;
        this.studentScholarshipService = studentScholarshipService;
        this.objectMapper = objectMapper;
    }

    /** Returns year-wise fees from the enquiry linked to this student (pre-fill data). */
    public List<YearFeeFromEnquiry> getEnquiryYearFees(Long studentId) {
        Enquiry enquiry = enquiryRepository.findByConvertedStudentId(studentId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No linked enquiry found for student: " + studentId));

        String json = enquiry.getYearWiseFees();
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

        StudentFeeAllocation saved = allocationRepository.save(allocation);

        List<SemesterFee> semesterFees = new ArrayList<>();
        for (StudentFeeAllocationRequest.YearFee yearFee : request.yearFees()) {
            BigDecimal sem1Amount = yearFee.amount().divide(BigDecimal.TWO, 2, RoundingMode.FLOOR);
            BigDecimal sem2Amount = yearFee.amount().subtract(sem1Amount);

            int globalSem1 = (yearFee.yearNumber() - 1) * 2 + 1;
            int globalSem2 = globalSem1 + 1;

            SemesterFee sf1 = new SemesterFee(
                saved, yearFee.yearNumber(),
                "Year " + yearFee.yearNumber() + " - " + installmentOrdinalLabel(globalSem1),
                sem1Amount, yearFee.dueDate(), 1
            );
            semesterFees.add(semesterFeeRepository.save(sf1));

            SemesterFee sf2 = new SemesterFee(
                saved, yearFee.yearNumber(),
                "Year " + yearFee.yearNumber() + " - " + installmentOrdinalLabel(globalSem2),
                sem2Amount, yearFee.dueDate().plusMonths(6), 2
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
        List<StudentFeeAllocationResponse.InstallmentFeeDetail> details = semesterFees.stream()
            .map(sf -> {
                BigDecimal paid = installmentRepository.sumAmountPaidBySemesterFeeId(sf.getId());
                BigDecimal pending = sf.getAmount().subtract(paid).max(BigDecimal.ZERO);
                BigDecimal penaltyAmount = penaltyRepository.findBySemesterFeeId(sf.getId()).stream()
                    .filter(p -> !p.getIsPaid())
                    .map(Penalty::getTotalPenalty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                String paymentStatus;
                if (pending.compareTo(BigDecimal.ZERO) <= 0) {
                    paymentStatus = "PAID";
                } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
                    paymentStatus = "PARTIAL";
                } else {
                    paymentStatus = "PENDING";
                }

                return new StudentFeeAllocationResponse.InstallmentFeeDetail(
                    sf.getId(), sf.getYearNumber(), sf.getSemesterSequence(), sf.getSemesterLabel(),
                    sf.getAmount(), sf.getDueDate(), paid, pending, penaltyAmount, paymentStatus
                );
            })
            .toList();

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
