package com.cms.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.cms.repository.StudentSpecification;

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
    private final PaymentCollectionService paymentCollectionService;

    public FeeExplorerService(StudentRepository studentRepository,
                               StudentFeeAllocationRepository allocationRepository,
                               SemesterFeeRepository semesterFeeRepository,
                               FeeInstallmentRepository installmentRepository,
                               PenaltyRepository penaltyRepository,
                               EnquiryRepository enquiryRepository,
                               EnquiryPaymentRepository enquiryPaymentRepository,
                               AdmissionRepository admissionRepository,
                               PaymentCollectionService paymentCollectionService) {
        this.studentRepository = studentRepository;
        this.allocationRepository = allocationRepository;
        this.semesterFeeRepository = semesterFeeRepository;
        this.installmentRepository = installmentRepository;
        this.penaltyRepository = penaltyRepository;
        this.enquiryRepository = enquiryRepository;
        this.enquiryPaymentRepository = enquiryPaymentRepository;
        this.admissionRepository = admissionRepository;
        this.paymentCollectionService = paymentCollectionService;
    }

    /**
     * Paginated version of {@link #search(String)} used by the fee explorer and collect-payment
     * list screens. The search term applies server-side; all other UI filters (program, academic
     * year, allocation status) are applied client-side within the returned page.
     */
    public Page<FeeExplorerResponse.StudentFeeSummary> searchPageable(String search, Pageable pageable) {
        Specification<Student> spec = Specification.where(null);
        if (search != null && search.length() >= 2) spec = spec.and(StudentSpecification.bySearch(search));

        // ID-only first pass — avoids the heavy JOIN on the full table and lets JPA handle count query.
        Pageable idPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        Page<Student> idPage = studentRepository.findAll(spec, idPageable);
        if (idPage.isEmpty()) return idPage.map(s -> buildSummary(s, null, null));

        // Fetch the full Student graph (relations) for just this page's IDs.
        List<Long> ids = idPage.getContent().stream().map(Student::getId).toList();
        Map<Long, Student> byId = studentRepository.findByIdInWithRelations(ids)
            .stream().collect(Collectors.toMap(Student::getId, s -> s));
        List<Student> pageStudents = ids.stream().map(byId::get).filter(Objects::nonNull).toList();

        Map<Long, Admission> admByStudent = admissionRepository
            .findByStudentIdInFetchJoiningYear(ids)
            .stream().collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a, (a, b) -> a));

        List<FeeExplorerResponse.StudentFeeSummary> content = pageStudents.stream()
            .map(s -> buildSummary(s, admByStudent.get(s.getId()), null))
            .toList();
        return new PageImpl<>(content, pageable, idPage.getTotalElements());
    }

    public FeeExplorerResponse search(String query) {
        List<Student> students = findStudents(query);

        List<Long> studentIds = students.stream().map(Student::getId).toList();
        Map<Long, Admission> admissionByStudentId = admissionRepository.findByStudentIdInFetchJoiningYear(studentIds)
            .stream().collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a, (a, b) -> a));

        List<FeeExplorerResponse.StudentFeeSummary> summaries = students.stream()
            .map(s -> buildSummary(s, admissionByStudentId.get(s.getId()), null))
            .collect(Collectors.toCollection(ArrayList::new));

        return new FeeExplorerResponse(summaries);
    }

    private FeeExplorerResponse.StudentFeeSummary buildSummary(Student student, Admission adm, Collection<Long> unused) {
        String programName = student.getCourse() != null ? student.getCourse().getName()
            : student.getProgram() != null ? student.getProgram().getName() : null;
        Integer yearOfStudy = student.getYearOfStudy();
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

            BigDecimal enquiryCredit = enquiryRepository.findByConvertedStudentId(student.getId())
                .map(e -> enquiryPaymentRepository.sumAmountPaidByEnquiryId(e.getId()))
                .orElse(BigDecimal.ZERO);

            BigDecimal totalPending = allocation.getNetFee()
                .subtract(totalPaid)
                .subtract(enquiryCredit)
                .max(BigDecimal.ZERO);

            BigDecimal collectibleOutstanding = paymentCollectionService.getCollectibleOutstanding(student);
            return new FeeExplorerResponse.StudentFeeSummary(
                student.getId(), student.getFullName(), student.getRollNumber(),
                programName,
                student.getProgram() != null ? student.getProgram().getDurationYears() : null,
                allocation.getNetFee(), totalPaid, totalPending, totalPenalty,
                allocation.getStatus().name(), yearOfStudy, academicYearName,
                collectibleOutstanding
            );
        }
        return new FeeExplorerResponse.StudentFeeSummary(
            student.getId(), student.getFullName(), student.getRollNumber(),
            programName,
            student.getProgram() != null ? student.getProgram().getDurationYears() : null,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            "NOT_ALLOCATED", yearOfStudy, academicYearName, BigDecimal.ZERO
        );
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
