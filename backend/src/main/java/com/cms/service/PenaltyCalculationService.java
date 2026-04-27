package com.cms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.PenaltyResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Penalty;
import com.cms.model.SemesterFee;
import com.cms.model.Student;
import com.cms.repository.FeeInstallmentRepository;
import com.cms.repository.PenaltyRepository;
import com.cms.repository.SemesterFeeRepository;
import com.cms.repository.StudentFeeAllocationRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class PenaltyCalculationService {

    private static final BigDecimal DAILY_PENALTY_RATE = new BigDecimal("100.00");

    private final PenaltyRepository penaltyRepository;
    private final SemesterFeeRepository semesterFeeRepository;
    private final FeeInstallmentRepository installmentRepository;
    private final StudentFeeAllocationRepository allocationRepository;
    private final StudentRepository studentRepository;

    public PenaltyCalculationService(PenaltyRepository penaltyRepository,
                                      SemesterFeeRepository semesterFeeRepository,
                                      FeeInstallmentRepository installmentRepository,
                                      StudentFeeAllocationRepository allocationRepository,
                                      StudentRepository studentRepository) {
        this.penaltyRepository = penaltyRepository;
        this.semesterFeeRepository = semesterFeeRepository;
        this.installmentRepository = installmentRepository;
        this.allocationRepository = allocationRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public PenaltyResponse calculatePenalties(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        var allocation = allocationRepository.findByStudentId(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Fee allocation not found for student: " + student.getRollNumber()));

        List<SemesterFee> semesterFees = semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(allocation.getId());
        LocalDate today = LocalDate.now();

        for (SemesterFee sf : semesterFees) {
            if (sf.getDueDate().isBefore(today)) {
                BigDecimal paid = installmentRepository.sumAmountPaidBySemesterFeeId(sf.getId());
                BigDecimal pending = sf.getAmount().subtract(paid);

                if (pending.compareTo(BigDecimal.ZERO) > 0) {
                    List<Penalty> existing = penaltyRepository.findBySemesterFeeId(sf.getId());
                    if (existing.isEmpty()) {
                        // Create new penalty starting from due date
                        long overdueDays = ChronoUnit.DAYS.between(sf.getDueDate(), today);
                        BigDecimal totalPenalty = DAILY_PENALTY_RATE.multiply(BigDecimal.valueOf(overdueDays));

                        Penalty penalty = new Penalty(sf, student, DAILY_PENALTY_RATE,
                            sf.getDueDate(), totalPenalty);
                        penalty.setPenaltyEndDate(today);
                        penaltyRepository.save(penalty);
                    } else {
                        // Update existing penalty
                        Penalty penalty = existing.getFirst();
                        if (!penalty.getIsPaid()) {
                            long overdueDays = ChronoUnit.DAYS.between(penalty.getPenaltyStartDate(), today);
                            penalty.setTotalPenalty(DAILY_PENALTY_RATE.multiply(BigDecimal.valueOf(overdueDays)));
                            penalty.setPenaltyEndDate(today);
                            penaltyRepository.save(penalty);
                        }
                    }
                }
            }
        }

        return getPenalties(studentId);
    }

    public PenaltyResponse getPenalties(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        List<Penalty> penalties = penaltyRepository.findByStudentId(studentId);

        BigDecimal totalPenalty = penalties.stream()
            .filter(p -> !p.getIsPaid())
            .map(Penalty::getTotalPenalty)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PenaltyResponse.PenaltyDetail> details = penalties.stream()
            .map(p -> {
                long overdueDays = 0;
                if (p.getPenaltyEndDate() != null) {
                    overdueDays = ChronoUnit.DAYS.between(p.getPenaltyStartDate(), p.getPenaltyEndDate());
                }
                return new PenaltyResponse.PenaltyDetail(
                    p.getId(), p.getSemesterFee().getId(),
                    p.getSemesterFee().getSemesterLabel(), p.getSemesterFee().getYearNumber(),
                    p.getDailyRate(), p.getPenaltyStartDate(), p.getPenaltyEndDate(),
                    overdueDays, p.getTotalPenalty(), p.getIsPaid()
                );
            })
            .toList();

        return new PenaltyResponse(student.getId(), student.getFullName(), student.getRollNumber(),
            totalPenalty, details);
    }
}
