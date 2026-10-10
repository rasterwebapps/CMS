package com.cms.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.LegacyFeeDemandBackfillApplyResult;
import com.cms.dto.LegacyFeeDemandBackfillCandidate;
import com.cms.dto.LegacyFeeDemandBackfillSummary;
import com.cms.dto.LegacyTermOverrideApplyResult;
import com.cms.dto.LegacyTermOverrideRow;
import com.cms.dto.LegacyTermOverrideSummary;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.FeeDemand;
import com.cms.model.Program;
import com.cms.model.SemesterFee;
import com.cms.model.Student;
import com.cms.model.StudentFeeAllocation;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.StudentTermFeeOverride;
import com.cms.model.TermBillingSchedule;
import com.cms.model.TermInstance;
import com.cms.model.enums.AssessmentPattern;
import com.cms.model.enums.DemandStatus;
import com.cms.model.enums.FeeAllocationStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.FeeDemandRepository;
import com.cms.repository.FeeInstallmentRepository;
import com.cms.repository.SemesterFeeRepository;
import com.cms.repository.StudentFeeAllocationRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.StudentTermFeeOverrideRepository;
import com.cms.repository.TermBillingScheduleRepository;
import com.cms.repository.TermInstanceRepository;
import com.cms.util.CurrentUserResolver;

@Service
@Transactional(readOnly = true)
public class LegacyFeeDemandBackfillServiceImpl implements LegacyFeeDemandBackfillService {

    private final StudentRepository studentRepository;
    private final TermInstanceRepository termInstanceRepository;
    private final StudentFeeAllocationRepository allocationRepository;
    private final SemesterFeeRepository semesterFeeRepository;
    private final FeeInstallmentRepository feeInstallmentRepository;
    private final TermBillingScheduleRepository billingScheduleRepository;
    private final StudentTermEnrollmentRepository enrollmentRepository;
    private final StudentTermFeeOverrideRepository termFeeOverrideRepository;
    private final FeeDemandRepository feeDemandRepository;
    private final StudentTermEnrollmentService enrollmentService;
    private final CurrentUserResolver currentUserResolver;

    public LegacyFeeDemandBackfillServiceImpl(StudentRepository studentRepository,
                                               TermInstanceRepository termInstanceRepository,
                                               StudentFeeAllocationRepository allocationRepository,
                                               SemesterFeeRepository semesterFeeRepository,
                                               FeeInstallmentRepository feeInstallmentRepository,
                                               TermBillingScheduleRepository billingScheduleRepository,
                                               StudentTermEnrollmentRepository enrollmentRepository,
                                               StudentTermFeeOverrideRepository termFeeOverrideRepository,
                                               FeeDemandRepository feeDemandRepository,
                                               StudentTermEnrollmentService enrollmentService,
                                               CurrentUserResolver currentUserResolver) {
        this.studentRepository = studentRepository;
        this.termInstanceRepository = termInstanceRepository;
        this.allocationRepository = allocationRepository;
        this.semesterFeeRepository = semesterFeeRepository;
        this.feeInstallmentRepository = feeInstallmentRepository;
        this.billingScheduleRepository = billingScheduleRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.termFeeOverrideRepository = termFeeOverrideRepository;
        this.feeDemandRepository = feeDemandRepository;
        this.enrollmentService = enrollmentService;
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public LegacyFeeDemandBackfillSummary auditCandidates(Long termInstanceId) {
        TermInstance termInstance = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found: " + termInstanceId));

        List<Student> activeStudents = studentRepository.findByStatus(StudentStatus.ACTIVE);
        List<LegacyFeeDemandBackfillCandidate> rows = new ArrayList<>();
        for (Student student : activeStudents) {
            rows.add(evaluateCandidate(student, termInstance));
        }

        int ok = (int) rows.stream().filter(LegacyFeeDemandBackfillCandidate::ok).count();
        return new LegacyFeeDemandBackfillSummary(activeStudents.size(), ok, rows.size() - ok, rows);
    }

    private LegacyFeeDemandBackfillCandidate evaluateCandidate(Student student, TermInstance termInstance) {
        Cohort cohort = student.getCohort();
        if (cohort == null) {
            return exceptionRow(student, null, null, null, "No cohort assigned");
        }

        Integer semesterNumber = enrollmentService.computeSemesterNumber(cohort, termInstance);
        if (semesterNumber == null) {
            return exceptionRow(student, cohort.getId(), null, null,
                "Outside this program's term range for this term instance");
        }

        AssessmentPattern pattern = cohort.getProgram().getAssessmentPattern();
        int yearOfStudy = (pattern == AssessmentPattern.YEARLY)
            ? semesterNumber
            : (int) Math.ceil(semesterNumber / 2.0);

        Optional<StudentFeeAllocation> allocationOpt = allocationRepository.findByStudentId(student.getId());
        if (allocationOpt.isEmpty() || allocationOpt.get().getStatus() != FeeAllocationStatus.FINALIZED) {
            return exceptionRow(student, cohort.getId(), semesterNumber, yearOfStudy,
                "No finalized legacy fee allocation");
        }
        StudentFeeAllocation allocation = allocationOpt.get();

        Optional<SemesterFee> semesterFeeOpt = resolveLegacySemesterFee(allocation, pattern, semesterNumber);
        if (semesterFeeOpt.isEmpty()) {
            return exceptionRow(student, cohort.getId(), semesterNumber, yearOfStudy,
                "No legacy SemesterFee row for this term");
        }
        SemesterFee semesterFee = semesterFeeOpt.get();

        BigDecimal legacyAmount = semesterFee.getAmount();
        BigDecimal legacyPaid = feeInstallmentRepository.sumAmountPaidBySemesterFeeId(semesterFee.getId());

        return new LegacyFeeDemandBackfillCandidate(student.getId(), student.getFullName(), cohort.getId(),
            semesterNumber, yearOfStudy, legacyAmount, legacyPaid, true, null);
    }

    private LegacyFeeDemandBackfillCandidate exceptionRow(Student student, Long cohortId, Integer semesterNumber,
                                                            Integer yearOfStudy, String reason) {
        return new LegacyFeeDemandBackfillCandidate(student.getId(), student.getFullName(), cohortId,
            semesterNumber, yearOfStudy, null, null, false, reason);
    }

    /** Mirrors RetroAdmitService's own SemesterFee creation: YEARLY programs save one row per
     *  year at sequence 1 ("Year N - Annual"); TERM_BASED programs split each year into ODD
     *  semesterNumbers (1,3,5,7) at sequence 1 and EVEN (2,4,6,8) at sequence 2. */
    private Optional<SemesterFee> resolveLegacySemesterFee(StudentFeeAllocation allocation, AssessmentPattern pattern,
                                                             int semesterNumber) {
        int yearNumber;
        int semesterSequence;
        if (pattern == AssessmentPattern.YEARLY) {
            yearNumber = semesterNumber;
            semesterSequence = 1;
        } else {
            yearNumber = (semesterNumber + 1) / 2;
            semesterSequence = ((semesterNumber - 1) % 2) + 1;
        }
        return semesterFeeRepository.findByAllocationIdAndYearNumberAndSemesterSequence(
            allocation.getId(), yearNumber, semesterSequence);
    }

    @Override
    @Transactional
    public LegacyFeeDemandBackfillApplyResult applyBackfill(Long termInstanceId) {
        TermInstance termInstance = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found: " + termInstanceId));
        AcademicYear academicYear = termInstance.getAcademicYear();
        TermBillingSchedule billingSchedule = billingScheduleRepository
            .findByAcademicYearIdAndTermType(academicYear.getId(), termInstance.getTermType())
            .orElseThrow(() -> new IllegalStateException(
                "No billing schedule configured for " + academicYear.getName() + " " + termInstance.getTermType()
                + " -- configure one before running the backfill."));

        // Recomputed fresh, server-side -- never trusts a client-supplied list of "OK" ids.
        LegacyFeeDemandBackfillSummary audit = auditCandidates(termInstanceId);
        List<LegacyFeeDemandBackfillCandidate> okCandidates = audit.candidates().stream()
            .filter(LegacyFeeDemandBackfillCandidate::ok)
            .toList();

        // Reused as-is -- a plain per-cohort/per-student enrollment-row creation loop with no
        // fee-structure dependency, so it cannot fail on another student's unrelated data gap.
        int enrollmentsCreated = enrollmentService.generateEnrollmentsForTermInstance(termInstanceId);

        String actor = currentUserResolver.resolve();
        int demandsCreated = 0;
        for (LegacyFeeDemandBackfillCandidate candidate : okCandidates) {
            // Kept as a consistency safety net: if this term's demands are ever regenerated later
            // through the normal (unscoped) FeeDemandService path, the override makes it reuse this
            // same reconciled amount instead of a fresh, possibly-different FeeStructureGroup calc.
            StudentTermFeeOverride override = termFeeOverrideRepository
                .findByStudentIdAndSemesterNumber(candidate.studentId(), candidate.semesterNumber())
                .orElseGet(StudentTermFeeOverride::new);
            if (override.getId() == null) {
                override.setStudent(studentRepository.getReferenceById(candidate.studentId()));
                override.setSemesterNumber(candidate.semesterNumber());
            }
            override.setOverrideAmount(candidate.legacyAmount());
            override.setSetBy(actor);
            override.setSetAt(Instant.now());
            termFeeOverrideRepository.save(override);

            Optional<StudentTermEnrollment> enrollmentOpt =
                enrollmentRepository.findByStudentIdAndTermInstanceId(candidate.studentId(), termInstanceId);
            if (enrollmentOpt.isEmpty()) {
                continue;
            }
            StudentTermEnrollment enrollment = enrollmentOpt.get();

            // Deliberately NOT FeeDemandService.generateDemandsForTermInstance here: that method
            // loops over every ENROLLED student in the term instance, not just this backfill's
            // audited candidates, and has no per-student error isolation -- one unrelated enrolled
            // student missing a FeeStructureGroup/Admission throws and rolls back the whole batch
            // (reproduced locally against student 46). Creating the FeeDemand directly, scoped to
            // only the OK candidates and already carrying the exact reconciled amount, can never be
            // affected by another student's unrelated data gap. Idempotent: an existing demand for
            // this enrollment is left untouched (it may already carry real post-migration payments
            // from live collection activity -- never overwrite those on a re-run).
            if (feeDemandRepository.findByStudentTermEnrollmentId(enrollment.getId()).isPresent()) {
                continue;
            }

            BigDecimal cappedPaid = candidate.legacyPaid().min(candidate.legacyAmount());
            FeeDemand demand = new FeeDemand();
            demand.setStudentTermEnrollment(enrollment);
            demand.setTermInstance(termInstance);
            demand.setAcademicYear(academicYear);
            demand.setTotalAmount(candidate.legacyAmount());
            demand.setDueDate(billingSchedule.getDueDate());
            demand.setPaidAmount(cappedPaid);
            demand.setStatus(resolveStatus(cappedPaid, candidate.legacyAmount()));
            feeDemandRepository.save(demand);
            demandsCreated++;
        }

        List<String> exceptionStudents = audit.candidates().stream()
            .filter(c -> !c.ok())
            .map(c -> c.studentId() + " (" + c.studentName() + "): " + c.exceptionReason())
            .toList();

        return new LegacyFeeDemandBackfillApplyResult(enrollmentsCreated, demandsCreated, demandsCreated, exceptionStudents);
    }

    private static DemandStatus resolveStatus(BigDecimal paid, BigDecimal total) {
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            return DemandStatus.UNPAID;
        }
        if (paid.compareTo(total) >= 0) {
            return DemandStatus.PAID;
        }
        return DemandStatus.PARTIAL;
    }

    @Override
    public LegacyTermOverrideSummary auditFutureTermOverrides(Long termInstanceId) {
        TermInstance termInstance = termInstanceRepository.findById(termInstanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Term instance not found: " + termInstanceId));

        List<Student> activeStudents = studentRepository.findByStatus(StudentStatus.ACTIVE);
        List<LegacyTermOverrideRow> rows = new ArrayList<>();
        for (Student student : activeStudents) {
            rows.addAll(evaluateRemainingTerms(student, termInstance));
        }

        int ok = (int) rows.stream().filter(LegacyTermOverrideRow::ok).count();
        return new LegacyTermOverrideSummary(rows.size(), ok, rows.size() - ok, rows);
    }

    /** Every term from the student's current one (per this term instance) through the end of
     *  their program, each resolved against their own legacy SemesterFee schedule -- empty (not
     *  an exception row) for a student with no cohort, no current-term mapping, or no finalized
     *  legacy allocation, since those are already reported by {@link #auditCandidates}. */
    private List<LegacyTermOverrideRow> evaluateRemainingTerms(Student student, TermInstance termInstance) {
        Cohort cohort = student.getCohort();
        if (cohort == null) {
            return List.of();
        }
        Integer currentSemesterNumber = enrollmentService.computeSemesterNumber(cohort, termInstance);
        if (currentSemesterNumber == null) {
            return List.of();
        }
        Optional<StudentFeeAllocation> allocationOpt = allocationRepository.findByStudentId(student.getId());
        if (allocationOpt.isEmpty() || allocationOpt.get().getStatus() != FeeAllocationStatus.FINALIZED) {
            return List.of();
        }
        StudentFeeAllocation allocation = allocationOpt.get();

        Program program = cohort.getProgram();
        AssessmentPattern pattern = program.getAssessmentPattern();
        Integer totalTerms = program.getTotalTerms();
        int lastTerm = totalTerms != null ? totalTerms : currentSemesterNumber;

        List<LegacyTermOverrideRow> rows = new ArrayList<>();
        for (int semNum = currentSemesterNumber; semNum <= lastTerm; semNum++) {
            int yearOfStudy = (pattern == AssessmentPattern.YEARLY) ? semNum : (int) Math.ceil(semNum / 2.0);
            Optional<SemesterFee> semesterFeeOpt = resolveLegacySemesterFee(allocation, pattern, semNum);
            if (semesterFeeOpt.isPresent()) {
                rows.add(new LegacyTermOverrideRow(student.getId(), student.getFullName(), semNum, yearOfStudy,
                    semesterFeeOpt.get().getAmount(), true, null));
            } else {
                rows.add(new LegacyTermOverrideRow(student.getId(), student.getFullName(), semNum, yearOfStudy,
                    null, false, "No legacy SemesterFee row for this term"));
            }
        }
        return rows;
    }

    @Override
    @Transactional
    public LegacyTermOverrideApplyResult applyFutureTermOverrides(Long termInstanceId) {
        // Recomputed fresh, server-side, same discipline as applyBackfill.
        LegacyTermOverrideSummary audit = auditFutureTermOverrides(termInstanceId);
        String actor = currentUserResolver.resolve();

        int written = 0;
        for (LegacyTermOverrideRow row : audit.rows()) {
            if (!row.ok()) {
                continue;
            }
            StudentTermFeeOverride override = termFeeOverrideRepository
                .findByStudentIdAndSemesterNumber(row.studentId(), row.semesterNumber())
                .orElseGet(StudentTermFeeOverride::new);
            if (override.getId() == null) {
                override.setStudent(studentRepository.getReferenceById(row.studentId()));
                override.setSemesterNumber(row.semesterNumber());
            }
            // Always pinned to the legacy amount -- the whole point of this operation is that the
            // originally-allocated fee structure wins, regardless of any live fee-structure change
            // made since, so an existing override (even one set by this same method previously) is
            // intentionally overwritten rather than left alone.
            override.setOverrideAmount(row.legacyAmount());
            override.setSetBy(actor);
            override.setSetAt(Instant.now());
            termFeeOverrideRepository.save(override);
            written++;
        }

        List<String> exceptionDetails = audit.rows().stream()
            .filter(r -> !r.ok())
            .map(r -> r.studentId() + " (" + r.studentName() + ") term " + r.semesterNumber() + ": " + r.exceptionReason())
            .toList();

        return new LegacyTermOverrideApplyResult(written, exceptionDetails);
    }
}
