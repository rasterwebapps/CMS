package com.cms.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

import com.cms.dto.FeeExplorerResponse;
import com.cms.model.Admission;
import com.cms.model.Penalty;
import com.cms.model.SemesterFee;
import com.cms.model.Student;
import com.cms.model.StudentFeeAllocation;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FeeInstallmentRepository;
import com.cms.repository.PenaltyRepository;
import com.cms.repository.SemesterFeeRepository;
import com.cms.repository.StudentFeeAllocationRepository;
import com.cms.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class FeeExplorerService {

    private final StudentRepository studentRepository;
    private final StudentFeeAllocationRepository allocationRepository;
    private final SemesterFeeRepository semesterFeeRepository;
    private final FeeInstallmentRepository installmentRepository;
    private final PenaltyRepository penaltyRepository;
    private final EnquiryRepository enquiryRepository;
    private final EnquiryPaymentRepository enquiryPaymentRepository;
    private final AdmissionRepository admissionRepository;

    public FeeExplorerService(StudentRepository studentRepository,
                               StudentFeeAllocationRepository allocationRepository,
                               SemesterFeeRepository semesterFeeRepository,
                               FeeInstallmentRepository installmentRepository,
                               PenaltyRepository penaltyRepository,
                               EnquiryRepository enquiryRepository,
                               EnquiryPaymentRepository enquiryPaymentRepository,
                               AdmissionRepository admissionRepository) {
        this.studentRepository = studentRepository;
        this.allocationRepository = allocationRepository;
        this.semesterFeeRepository = semesterFeeRepository;
        this.installmentRepository = installmentRepository;
        this.penaltyRepository = penaltyRepository;
        this.enquiryRepository = enquiryRepository;
        this.enquiryPaymentRepository = enquiryPaymentRepository;
        this.admissionRepository = admissionRepository;
    }

    public FeeExplorerResponse search(String query) {
        List<Student> students = findStudents(query);

        List<Long> studentIds = students.stream().map(Student::getId).toList();
        Map<Long, Admission> admissionByStudentId = admissionRepository.findByStudentIdIn(studentIds)
            .stream().collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a, (a, b) -> a));

        List<FeeExplorerResponse.StudentFeeSummary> summaries = new ArrayList<>();
        for (Student student : students) {
            String programName = student.getCourse() != null ? student.getCourse().getName()
                : student.getProgram() != null ? student.getProgram().getName() : null;
            Integer yearOfStudy = student.getYearOfStudy();
            Admission adm = admissionByStudentId.get(student.getId());
            String academicYearName = adm != null && adm.getJoiningAcademicYear() != null
                ? adm.getJoiningAcademicYear().getName() : null;

            var allocationOpt = allocationRepository.findByStudentId(student.getId());
            if (allocationOpt.isPresent()) {
                StudentFeeAllocation allocation = allocationOpt.get();
                List<SemesterFee> semesterFees = semesterFeeRepository
                    .findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(allocation.getId());

                BigDecimal totalPaid = BigDecimal.ZERO;
                BigDecimal totalPenalty = BigDecimal.ZERO;

                for (SemesterFee sf : semesterFees) {
                    totalPaid = totalPaid.add(installmentRepository.sumAmountPaidBySemesterFeeId(sf.getId()));
                    totalPenalty = totalPenalty.add(
                        penaltyRepository.findBySemesterFeeId(sf.getId()).stream()
                            .filter(p -> !p.getIsPaid())
                            .map(Penalty::getTotalPenalty)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                    );
                }

                // Enquiry pre-payments act as credit and reduce the true outstanding balance
                BigDecimal enquiryCredit = enquiryRepository.findByConvertedStudentId(student.getId())
                    .map(e -> enquiryPaymentRepository.sumAmountPaidByEnquiryId(e.getId()))
                    .orElse(BigDecimal.ZERO);

                BigDecimal totalPending = allocation.getNetFee()
                    .subtract(totalPaid)
                    .subtract(enquiryCredit)
                    .max(BigDecimal.ZERO);

                summaries.add(new FeeExplorerResponse.StudentFeeSummary(
                    student.getId(), student.getFullName(), student.getRollNumber(),
                    programName,
                    student.getProgram() != null ? student.getProgram().getDurationYears() : null,
                    allocation.getNetFee(), totalPaid, totalPending, totalPenalty,
                    allocation.getStatus().name(), yearOfStudy, academicYearName
                ));
            } else {
                summaries.add(new FeeExplorerResponse.StudentFeeSummary(
                    student.getId(), student.getFullName(), student.getRollNumber(),
                    programName,
                    student.getProgram() != null ? student.getProgram().getDurationYears() : null,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    "NOT_ALLOCATED", yearOfStudy, academicYearName
                ));
            }
        }

        return new FeeExplorerResponse(summaries);
    }

    private List<Student> findStudents(String query) {
        if (query == null || query.isBlank()) {
            return studentRepository.findAll();
        }

        // Try roll number first
        var byRoll = studentRepository.findByRollNumber(query.trim());
        if (byRoll.isPresent()) {
            return List.of(byRoll.get());
        }

        // Try program ID
        try {
            Long programId = Long.parseLong(query.trim());
            List<Student> byProgram = studentRepository.findByProgramId(programId);
            if (!byProgram.isEmpty()) {
                return byProgram;
            }
        } catch (NumberFormatException ignored) {
            // Not a number, continue
        }

        // Fallback: search by roll number containing (partial match)
        return studentRepository.findByRollNumberContainingIgnoreCase(query.trim());
    }
}
