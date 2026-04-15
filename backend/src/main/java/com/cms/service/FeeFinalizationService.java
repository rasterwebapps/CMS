package com.cms.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.StudentFeeAllocationRequest;
import com.cms.dto.StudentFeeAllocationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Penalty;
import com.cms.model.SemesterFee;
import com.cms.model.Student;
import com.cms.model.StudentFeeAllocation;
import com.cms.model.enums.FeeAllocationStatus;
import com.cms.repository.FeeInstallmentRepository;
import com.cms.repository.PenaltyRepository;
import com.cms.repository.SemesterFeeRepository;
import com.cms.repository.StudentFeeAllocationRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class FeeFinalizationService {

    private final StudentFeeAllocationRepository allocationRepository;
    private final SemesterFeeRepository semesterFeeRepository;
    private final FeeInstallmentRepository installmentRepository;
    private final PenaltyRepository penaltyRepository;
    private final StudentRepository studentRepository;

    public FeeFinalizationService(StudentFeeAllocationRepository allocationRepository,
                                   SemesterFeeRepository semesterFeeRepository,
                                   FeeInstallmentRepository installmentRepository,
                                   PenaltyRepository penaltyRepository,
                                   StudentRepository studentRepository) {
        this.allocationRepository = allocationRepository;
        this.semesterFeeRepository = semesterFeeRepository;
        this.installmentRepository = installmentRepository;
        this.penaltyRepository = penaltyRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public StudentFeeAllocationResponse finalize(StudentFeeAllocationRequest request, String adminUsername) {
        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.studentId()));

        if (allocationRepository.existsByStudentId(request.studentId())) {
            throw new IllegalStateException("Fee allocation already exists for student: " + student.getRollNumber());
        }

        BigDecimal discount = request.discountAmount() != null ? request.discountAmount() : BigDecimal.ZERO;
        BigDecimal commission = request.agentCommission() != null ? request.agentCommission() : BigDecimal.ZERO;
        BigDecimal netFee = request.totalFee().subtract(discount);

        StudentFeeAllocation allocation = new StudentFeeAllocation(
            student, student.getProgram(), request.totalFee(),
            discount, request.discountReason(), commission, netFee,
            FeeAllocationStatus.FINALIZED
        );
        allocation.setFinalizedAt(Instant.now());
        allocation.setFinalizedBy(adminUsername);

        StudentFeeAllocation saved = allocationRepository.save(allocation);

        List<SemesterFee> semesterFees = new ArrayList<>();
        for (StudentFeeAllocationRequest.YearFee yearFee : request.yearFees()) {
            SemesterFee sf = new SemesterFee(
                saved, yearFee.yearNumber(),
                "Year " + yearFee.yearNumber(), yearFee.amount(), yearFee.dueDate()
            );
            semesterFees.add(semesterFeeRepository.save(sf));
        }

        return toResponse(saved, semesterFees);
    }

    public StudentFeeAllocationResponse getByStudentId(Long studentId) {
        StudentFeeAllocation allocation = allocationRepository.findByStudentId(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Fee allocation not found for student id: " + studentId));

        List<SemesterFee> semesterFees = semesterFeeRepository.findByAllocationIdOrderByYearNumber(allocation.getId());
        return toResponse(allocation, semesterFees);
    }

    public StudentFeeAllocationResponse getById(Long id) {
        StudentFeeAllocation allocation = allocationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fee allocation not found with id: " + id));

        List<SemesterFee> semesterFees = semesterFeeRepository.findByAllocationIdOrderByYearNumber(allocation.getId());
        return toResponse(allocation, semesterFees);
    }

    private StudentFeeAllocationResponse toResponse(StudentFeeAllocation allocation, List<SemesterFee> semesterFees) {
        List<StudentFeeAllocationResponse.SemesterFeeDetail> details = semesterFees.stream()
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

                return new StudentFeeAllocationResponse.SemesterFeeDetail(
                    sf.getId(), sf.getYearNumber(), sf.getSemesterLabel(),
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
}
