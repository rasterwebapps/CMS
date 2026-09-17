package com.cms.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.FeeExplorerResponse;
import com.cms.dto.FeeExplorerSemesterWiseRow;
import com.cms.model.Admission;
import com.cms.model.Enquiry;
import com.cms.model.Penalty;
import com.cms.model.SemesterFee;
import com.cms.model.Student;
import com.cms.model.StudentFeeAllocation;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.EnquiryCreditApplicationRepository;
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
    private final EnquiryCreditApplicationRepository creditApplicationRepository;
    private final AdmissionRepository admissionRepository;
    private final PaymentCollectionService paymentCollectionService;

    public FeeExplorerService(StudentRepository studentRepository,
                               StudentFeeAllocationRepository allocationRepository,
                               SemesterFeeRepository semesterFeeRepository,
                               FeeInstallmentRepository installmentRepository,
                               PenaltyRepository penaltyRepository,
                               EnquiryRepository enquiryRepository,
                               EnquiryPaymentRepository enquiryPaymentRepository,
                               EnquiryCreditApplicationRepository creditApplicationRepository,
                               AdmissionRepository admissionRepository,
                               PaymentCollectionService paymentCollectionService) {
        this.studentRepository = studentRepository;
        this.allocationRepository = allocationRepository;
        this.semesterFeeRepository = semesterFeeRepository;
        this.installmentRepository = installmentRepository;
        this.penaltyRepository = penaltyRepository;
        this.enquiryRepository = enquiryRepository;
        this.enquiryPaymentRepository = enquiryPaymentRepository;
        this.creditApplicationRepository = creditApplicationRepository;
        this.admissionRepository = admissionRepository;
        this.paymentCollectionService = paymentCollectionService;
    }

    /**
     * Paginated version of {@link #search(String)} used by the fee explorer and collect-payment
     * list screens. When none of the dropdown filters (program, academic year, year of study,
     * allocation status) are active, the search term is pushed to the DB and pagination happens
     * there. Once any dropdown filter is active, those dimensions are only known after computing
     * each row's summary (they are not plain Student columns), so this falls back to computing
     * every search-matching row via {@link #searchAll} and paginating the filtered result in
     * memory — the same approach already used, unbounded, for the export endpoint.
     */
    public Page<FeeExplorerResponse.StudentFeeSummary> searchPageable(
            String search, String program, String academicYear, Integer yearOfStudy, String allocationStatus,
            Pageable pageable) {
        boolean hasDropdownFilter =
            (program != null && !program.isBlank() && !program.equals("ALL"))
            || (academicYear != null && !academicYear.isBlank() && !academicYear.equals("ALL"))
            || yearOfStudy != null
            || (allocationStatus != null && !allocationStatus.isBlank() && !allocationStatus.equals("ALL"));

        if (!hasDropdownFilter) return searchPageable(search, pageable);

        List<FeeExplorerResponse.StudentFeeSummary> filtered =
            searchAll(search, program, academicYear, yearOfStudy, allocationStatus, pageable.getSort());
        int total = filtered.size();
        int from = Math.min((int) pageable.getOffset(), total);
        int to = Math.min(from + pageable.getPageSize(), total);
        return new PageImpl<>(filtered.subList(from, to), pageable, total);
    }

    /** Search-only fast path — no dropdown filters active, so pagination happens at the DB. */
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

    /** Distinct filter-dropdown values across every student, not just the currently loaded page. */
    public FilterOptions getFilterOptions() {
        List<Student> students = studentRepository.findAll();

        Set<String> programs = new TreeSet<>();
        Set<Integer> years = new TreeSet<>();
        for (Student s : students) {
            String name = s.getCourse() != null ? s.getCourse().getName()
                : s.getProgram() != null ? s.getProgram().getName() : null;
            if (name != null) programs.add(name);
            if (s.getYearOfStudy() != null) years.add(s.getYearOfStudy());
        }

        List<Long> ids = students.stream().map(Student::getId).toList();
        Set<String> academicYears = admissionRepository.findByStudentIdInFetchJoiningYear(ids).stream()
            .map(a -> a.getJoiningAcademicYear() != null ? a.getJoiningAcademicYear().getName() : null)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(TreeSet::new));

        return new FilterOptions(new ArrayList<>(programs), new ArrayList<>(academicYears), new ArrayList<>(years));
    }

    public record FilterOptions(List<String> programs, List<String> academicYears, List<Integer> yearsOfStudy) {}

    /**
     * Unbounded version for export: applies the search spec then post-filters by the client-side
     * dimensions (program name, academic year name, yearOfStudy, allocationStatus).
     */
    public List<FeeExplorerResponse.StudentFeeSummary> searchAll(
            String search, String program, String academicYear, Integer yearOfStudy, String allocationStatus, Sort sort) {

        Specification<Student> spec = Specification.where(null);
        if (search != null && search.length() >= 2) spec = spec.and(StudentSpecification.bySearch(search));

        List<Student> students = studentRepository.findAll(spec, sort);
        List<Long> ids = students.stream().map(Student::getId).toList();

        Map<Long, Admission> admByStudent = admissionRepository
            .findByStudentIdInFetchJoiningYear(ids)
            .stream().collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a, (a, b) -> a));

        return students.stream()
            .map(s -> buildSummary(s, admByStudent.get(s.getId()), null))
            .filter(r -> {
                if (program != null && !program.isBlank() && !program.equals("ALL")
                        && !program.equalsIgnoreCase(r.programName())) return false;
                if (academicYear != null && !academicYear.isBlank() && !academicYear.equals("ALL")
                        && !academicYear.equalsIgnoreCase(r.academicYearName())) return false;
                if (yearOfStudy != null && !Objects.equals(r.yearOfStudy(), yearOfStudy)) return false;
                if (allocationStatus != null && !allocationStatus.isBlank() && !allocationStatus.equals("ALL")
                        && !allocationStatus.equalsIgnoreCase(r.allocationStatus())) return false;
                return true;
            })
            .toList();
    }

    /**
     * Semester-wise breakdown for the Fee Explorer "Sem-wise" export: one row per student per
     * semester instead of {@link #searchAll}'s per-student aggregate, so a client can see exactly
     * which semester(s) still have a pending balance. Reuses {@link #searchAll} for filtering
     * (same search/program/academicYear/yearOfStudy/allocationStatus semantics as the existing
     * export) and {@link #computeSemesterAmounts} for the per-semester paid/pending figures, so
     * this can never drift from the aggregate numbers shown elsewhere.
     */
    public List<FeeExplorerSemesterWiseRow> searchAllSemesterWise(
            String search, String program, String academicYear, Integer yearOfStudy, String allocationStatus, Sort sort) {

        List<FeeExplorerResponse.StudentFeeSummary> summaries =
            searchAll(search, program, academicYear, yearOfStudy, allocationStatus, sort);

        List<FeeExplorerSemesterWiseRow> rows = new ArrayList<>();
        for (FeeExplorerResponse.StudentFeeSummary summary : summaries) {
            if ("NOT_ALLOCATED".equals(summary.allocationStatus())) continue;

            Student student = studentRepository.findById(summary.studentId()).orElse(null);
            var allocationOpt = student != null ? allocationRepository.findByStudentId(student.getId()) : Optional.<StudentFeeAllocation>empty();
            if (student == null || allocationOpt.isEmpty()) continue;

            StudentFeeAllocation allocation = allocationOpt.get();
            List<SemesterFee> semesterFees = semesterFeeRepository
                .findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(allocation.getId());
            SemesterAmountsResult amounts = computeSemesterAmounts(student, semesterFees);

            for (SemesterFeeAmounts sa : amounts.perSemester()) {
                rows.add(new FeeExplorerSemesterWiseRow(
                    summary.studentId(), summary.rollNumber(), summary.studentName(), summary.programName(),
                    summary.academicYearName(), sa.semesterFee().getYearNumber(), sa.semesterFee().getSemesterLabel(),
                    sa.semesterFee().getAmount(), sa.paid(), sa.pending()
                ));
            }
        }
        return rows;
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

            SemesterAmountsResult amounts = computeSemesterAmounts(student, semesterFees);
            BigDecimal totalPaid = amounts.totalPaid();
            BigDecimal totalPenalty = amounts.totalPenalty();
            BigDecimal totalPending = allocation.getNetFee().subtract(totalPaid).max(BigDecimal.ZERO);

            BigDecimal collectibleOutstanding = paymentCollectionService.getCollectibleOutstanding(student);
            BigDecimal currentInstallmentDue = paymentCollectionService.getCurrentInstallmentDue(student);
            return new FeeExplorerResponse.StudentFeeSummary(
                student.getId(), student.getFullName(), student.getRollNumber(),
                programName,
                student.getProgram() != null ? student.getProgram().getDurationYears() : null,
                allocation.getNetFee(), totalPaid, totalPending, totalPenalty,
                allocation.getStatus().name(), yearOfStudy, academicYearName,
                collectibleOutstanding, currentInstallmentDue
            );
        }
        return new FeeExplorerResponse.StudentFeeSummary(
            student.getId(), student.getFullName(), student.getRollNumber(),
            programName,
            student.getProgram() != null ? student.getProgram().getDurationYears() : null,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            "NOT_ALLOCATED", yearOfStudy, academicYearName, BigDecimal.ZERO, BigDecimal.ZERO
        );
    }

    /** One semester's computed paid/pending/penalty, alongside the {@link SemesterFee} it came from. */
    private record SemesterFeeAmounts(SemesterFee semesterFee, BigDecimal paid, BigDecimal pending, BigDecimal penalty) {}

    private record SemesterAmountsResult(List<SemesterFeeAmounts> perSemester, BigDecimal totalPaid, BigDecimal totalPenalty) {}

    /**
     * Walks a student's semester fees in order, carrying forward any unapplied enquiry
     * pre-admission credit across semesters exactly as {@link #buildSummary} always has — the
     * single source of truth for per-semester paid/pending/penalty so the aggregate summary and
     * the semester-wise export can never disagree on the underlying numbers.
     */
    private SemesterAmountsResult computeSemesterAmounts(Student student, List<SemesterFee> semesterFees) {
        // Enquiry pre-admission credit is real money already received — it must count toward
        // "Paid" the same way PaymentCollectionService.calculateTotalOutstanding() and
        // FeeFinalizationService.toResponse() treat it, or Paid + Pending won't sum to Total Fee.
        Optional<Enquiry> sourceEnquiry = enquiryRepository.findByConvertedStudentId(student.getId());
        BigDecimal totalEnquiryCredit = sourceEnquiry
            .map(e -> enquiryPaymentRepository.sumAmountPaidByEnquiryId(e.getId()))
            .orElse(BigDecimal.ZERO);
        BigDecimal alreadyAppliedCredit = sourceEnquiry
            .map(e -> creditApplicationRepository.sumAmountAppliedByEnquiryId(e.getId()))
            .orElse(BigDecimal.ZERO);
        BigDecimal remainingEnquiryCredit = totalEnquiryCredit.subtract(alreadyAppliedCredit).max(BigDecimal.ZERO);

        List<SemesterFeeAmounts> perSemester = new ArrayList<>();
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalPenalty = BigDecimal.ZERO;

        for (SemesterFee sf : semesterFees) {
            BigDecimal installmentPaid = installmentRepository.sumAmountPaidBySemesterFeeId(sf.getId());
            BigDecimal alreadyCredited = sourceEnquiry.isPresent()
                ? creditApplicationRepository.sumAmountAppliedByEnquiryIdAndSemesterFeeId(
                    sourceEnquiry.get().getId(), sf.getId())
                : BigDecimal.ZERO;
            BigDecimal capacity = sf.getAmount().subtract(installmentPaid).subtract(alreadyCredited).max(BigDecimal.ZERO);
            BigDecimal creditForThis = remainingEnquiryCredit.min(capacity);
            remainingEnquiryCredit = remainingEnquiryCredit.subtract(creditForThis);

            BigDecimal semPaid = installmentPaid.add(alreadyCredited).add(creditForThis);
            BigDecimal semPending = sf.getAmount().subtract(semPaid).max(BigDecimal.ZERO);
            BigDecimal semPenalty = penaltyRepository.findBySemesterFeeId(sf.getId()).stream()
                .filter(p -> !p.getIsPaid())
                .map(Penalty::getTotalPenalty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            perSemester.add(new SemesterFeeAmounts(sf, semPaid, semPending, semPenalty));
            totalPaid = totalPaid.add(semPaid);
            totalPenalty = totalPenalty.add(semPenalty);
        }
        return new SemesterAmountsResult(perSemester, totalPaid, totalPenalty);
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
