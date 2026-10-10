package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.LegacyFeeDemandBackfillApplyResult;
import com.cms.dto.LegacyFeeDemandBackfillCandidate;
import com.cms.dto.LegacyFeeDemandBackfillSummary;
import com.cms.dto.LegacyTermOverrideApplyResult;
import com.cms.dto.LegacyTermOverrideSummary;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.Course;
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
import com.cms.model.enums.TermType;
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

/**
 * Covers the legacy-to-FeeDemand billing backfill's reconciliation logic: the TERM_BASED/YEARLY
 * SemesterFee mapping, the exception paths (no cohort, no legacy allocation), and the
 * paidAmount -> DemandStatus boundary conditions. See the backfill plan for why this exists: a
 * production billing migration that must never mismatch an existing legacy total/paid amount.
 */
@ExtendWith(MockitoExtension.class)
class LegacyFeeDemandBackfillServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private TermInstanceRepository termInstanceRepository;
    @Mock private StudentFeeAllocationRepository allocationRepository;
    @Mock private SemesterFeeRepository semesterFeeRepository;
    @Mock private FeeInstallmentRepository feeInstallmentRepository;
    @Mock private TermBillingScheduleRepository billingScheduleRepository;
    @Mock private StudentTermEnrollmentRepository enrollmentRepository;
    @Mock private StudentTermFeeOverrideRepository termFeeOverrideRepository;
    @Mock private FeeDemandRepository feeDemandRepository;
    @Mock private StudentTermEnrollmentService enrollmentService;
    @Mock private CurrentUserResolver currentUserResolver;

    private LegacyFeeDemandBackfillServiceImpl service;

    private TermInstance termInstance;

    @BeforeEach
    void setUp() {
        service = new LegacyFeeDemandBackfillServiceImpl(studentRepository, termInstanceRepository,
            allocationRepository, semesterFeeRepository, feeInstallmentRepository, billingScheduleRepository,
            enrollmentRepository, termFeeOverrideRepository, feeDemandRepository, enrollmentService,
            currentUserResolver);

        termInstance = new TermInstance();
        termInstance.setId(3L);
        AcademicYear ay = new AcademicYear("2026-2027", LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), true);
        ay.setId(2L);
        termInstance.setAcademicYear(ay);
        termInstance.setTermType(TermType.ODD);

        lenient().when(termInstanceRepository.findById(3L)).thenReturn(Optional.of(termInstance));
    }

    @Test
    void mapsTermBasedOddSemesterNumberToFirstSequenceOfItsYear() {
        Student student = activeStudent(1L, "A", TERM_BASED_PROGRAM);
        when(studentRepository.findByStatus(StudentStatus.ACTIVE)).thenReturn(List.of(student));
        when(enrollmentService.computeSemesterNumber(any(), eq(termInstance))).thenReturn(7);

        StudentFeeAllocation allocation = finalizedAllocation(student, 100L);
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(allocation));

        SemesterFee semesterFee = new SemesterFee(allocation, 4, "Year 4 - First Semester",
            new BigDecimal("75000.00"), LocalDate.of(2026, 12, 31), 1);
        semesterFee.setId(500L);
        when(semesterFeeRepository.findByAllocationIdAndYearNumberAndSemesterSequence(100L, 4, 1))
            .thenReturn(Optional.of(semesterFee));
        when(feeInstallmentRepository.sumAmountPaidBySemesterFeeId(500L)).thenReturn(new BigDecimal("50000.00"));

        LegacyFeeDemandBackfillSummary summary = service.auditCandidates(3L);

        assertThat(summary.okCount()).isEqualTo(1);
        LegacyFeeDemandBackfillCandidate row = summary.candidates().get(0);
        assertThat(row.ok()).isTrue();
        assertThat(row.semesterNumber()).isEqualTo(7);
        assertThat(row.yearOfStudy()).isEqualTo(4);
        assertThat(row.legacyAmount()).isEqualByComparingTo("75000.00");
        assertThat(row.legacyPaid()).isEqualByComparingTo("50000.00");
    }

    @Test
    void mapsTermBasedEvenSemesterNumberToSecondSequenceOfSameYear() {
        Student student = activeStudent(2L, "B", TERM_BASED_PROGRAM);
        when(studentRepository.findByStatus(StudentStatus.ACTIVE)).thenReturn(List.of(student));
        when(enrollmentService.computeSemesterNumber(any(), eq(termInstance))).thenReturn(8);

        StudentFeeAllocation allocation = finalizedAllocation(student, 101L);
        when(allocationRepository.findByStudentId(2L)).thenReturn(Optional.of(allocation));

        SemesterFee semesterFee = new SemesterFee(allocation, 4, "Year 4 - Second Semester",
            new BigDecimal("80000.00"), LocalDate.of(2027, 6, 30), 2);
        semesterFee.setId(501L);
        when(semesterFeeRepository.findByAllocationIdAndYearNumberAndSemesterSequence(101L, 4, 2))
            .thenReturn(Optional.of(semesterFee));
        when(feeInstallmentRepository.sumAmountPaidBySemesterFeeId(501L)).thenReturn(BigDecimal.ZERO);

        LegacyFeeDemandBackfillCandidate row = service.auditCandidates(3L).candidates().get(0);

        assertThat(row.ok()).isTrue();
        assertThat(row.yearOfStudy()).isEqualTo(4);
        assertThat(row.legacyAmount()).isEqualByComparingTo("80000.00");
    }

    @Test
    void mapsYearlyProgramSemesterNumberDirectlyToYearNumberAtSequenceOne() {
        Student student = activeStudent(3L, "C", YEARLY_PROGRAM);
        when(studentRepository.findByStatus(StudentStatus.ACTIVE)).thenReturn(List.of(student));
        when(enrollmentService.computeSemesterNumber(any(), eq(termInstance))).thenReturn(2);

        StudentFeeAllocation allocation = finalizedAllocation(student, 102L);
        when(allocationRepository.findByStudentId(3L)).thenReturn(Optional.of(allocation));

        SemesterFee semesterFee = new SemesterFee(allocation, 2, "Year 2 - Annual",
            new BigDecimal("90000.00"), LocalDate.of(2026, 6, 30), 1);
        semesterFee.setId(502L);
        when(semesterFeeRepository.findByAllocationIdAndYearNumberAndSemesterSequence(102L, 2, 1))
            .thenReturn(Optional.of(semesterFee));
        when(feeInstallmentRepository.sumAmountPaidBySemesterFeeId(502L)).thenReturn(new BigDecimal("90000.00"));

        LegacyFeeDemandBackfillCandidate row = service.auditCandidates(3L).candidates().get(0);

        assertThat(row.ok()).isTrue();
        assertThat(row.semesterNumber()).isEqualTo(2);
        assertThat(row.yearOfStudy()).isEqualTo(2);
        assertThat(row.legacyAmount()).isEqualByComparingTo("90000.00");
    }

    @Test
    void excludesStudentWithNoCohortAssigned() {
        Student student = new Student();
        student.setId(4L);
        student.setFirstName("D");
        student.setLastName("");
        student.setCohort(null);
        when(studentRepository.findByStatus(StudentStatus.ACTIVE)).thenReturn(List.of(student));

        LegacyFeeDemandBackfillCandidate row = service.auditCandidates(3L).candidates().get(0);

        assertThat(row.ok()).isFalse();
        assertThat(row.exceptionReason()).isEqualTo("No cohort assigned");
        verify(enrollmentService, never()).computeSemesterNumber(any(), any());
    }

    @Test
    void excludesStudentWithNoFinalizedLegacyAllocation() {
        Student student = activeStudent(5L, "E", TERM_BASED_PROGRAM);
        when(studentRepository.findByStatus(StudentStatus.ACTIVE)).thenReturn(List.of(student));
        when(enrollmentService.computeSemesterNumber(any(), eq(termInstance))).thenReturn(1);
        when(allocationRepository.findByStudentId(5L)).thenReturn(Optional.empty());

        LegacyFeeDemandBackfillCandidate row = service.auditCandidates(3L).candidates().get(0);

        assertThat(row.ok()).isFalse();
        assertThat(row.exceptionReason()).isEqualTo("No finalized legacy fee allocation");
    }

    @Test
    void excludesStudentMissingTheSpecificSemesterFeeRow() {
        Student student = activeStudent(6L, "F", TERM_BASED_PROGRAM);
        when(studentRepository.findByStatus(StudentStatus.ACTIVE)).thenReturn(List.of(student));
        when(enrollmentService.computeSemesterNumber(any(), eq(termInstance))).thenReturn(1);
        StudentFeeAllocation allocation = finalizedAllocation(student, 103L);
        when(allocationRepository.findByStudentId(6L)).thenReturn(Optional.of(allocation));
        when(semesterFeeRepository.findByAllocationIdAndYearNumberAndSemesterSequence(103L, 1, 1))
            .thenReturn(Optional.empty());

        LegacyFeeDemandBackfillCandidate row = service.auditCandidates(3L).candidates().get(0);

        assertThat(row.ok()).isFalse();
        assertThat(row.exceptionReason()).contains("No legacy SemesterFee row");
    }

    @Test
    void applyBackfillReconcilesPaidAmountAndStatusAcrossBoundaryConditions() {
        // Three OK students: unpaid, fully paid exactly, and overpaid (defensive cap).
        Student unpaid = activeStudent(10L, "Unpaid", TERM_BASED_PROGRAM);
        Student fullyPaid = activeStudent(11L, "FullyPaid", TERM_BASED_PROGRAM);
        Student overpaid = activeStudent(12L, "Overpaid", TERM_BASED_PROGRAM);
        when(studentRepository.findByStatus(StudentStatus.ACTIVE)).thenReturn(List.of(unpaid, fullyPaid, overpaid));
        when(enrollmentService.computeSemesterNumber(any(), eq(termInstance))).thenReturn(1);

        stubFinalizedAllocationChain(unpaid, 200L, 700L, "40000.00", "0.00");
        stubFinalizedAllocationChain(fullyPaid, 201L, 701L, "40000.00", "40000.00");
        stubFinalizedAllocationChain(overpaid, 202L, 702L, "40000.00", "45000.00");

        when(billingScheduleRepository.findByAcademicYearIdAndTermType(2L, TermType.ODD))
            .thenReturn(Optional.of(billingScheduleWithDueDate("2026-12-31")));
        when(enrollmentService.generateEnrollmentsForTermInstance(3L)).thenReturn(3);

        stubEnrollment(unpaid.getId(), 900L);
        stubEnrollment(fullyPaid.getId(), 901L);
        stubEnrollment(overpaid.getId(), 902L);
        // No FeeDemand exists yet for any of them -- findByStudentTermEnrollmentId's unstubbed
        // default (empty Optional) is correct here, triggering creation for all three.

        LegacyFeeDemandBackfillApplyResult result = service.applyBackfill(3L);

        assertThat(result.demandsCreated()).isEqualTo(3);
        assertThat(result.reconciled()).isEqualTo(3);
        assertThat(result.exceptionStudents()).isEmpty();

        ArgumentCaptor<FeeDemand> captor = ArgumentCaptor.forClass(FeeDemand.class);
        verify(feeDemandRepository, org.mockito.Mockito.times(3)).save(captor.capture());

        FeeDemand unpaidDemand = findByEnrollmentId(captor.getAllValues(), 900L);
        assertThat(unpaidDemand.getTotalAmount()).isEqualByComparingTo("40000.00");
        assertThat(unpaidDemand.getPaidAmount()).isEqualByComparingTo("0.00");
        assertThat(unpaidDemand.getStatus()).isEqualTo(DemandStatus.UNPAID);

        FeeDemand fullyPaidDemand = findByEnrollmentId(captor.getAllValues(), 901L);
        assertThat(fullyPaidDemand.getPaidAmount()).isEqualByComparingTo("40000.00");
        assertThat(fullyPaidDemand.getStatus()).isEqualTo(DemandStatus.PAID);

        FeeDemand overpaidDemand = findByEnrollmentId(captor.getAllValues(), 902L);
        assertThat(overpaidDemand.getPaidAmount()).isEqualByComparingTo("40000.00");
        assertThat(overpaidDemand.getStatus()).isEqualTo(DemandStatus.PAID);
    }

    @Test
    void applyBackfillSkipsStudentsWhoAlreadyHaveADemandForThisTermOnReRun() {
        Student student = activeStudent(20L, "AlreadyMigrated", TERM_BASED_PROGRAM);
        when(studentRepository.findByStatus(StudentStatus.ACTIVE)).thenReturn(List.of(student));
        when(enrollmentService.computeSemesterNumber(any(), eq(termInstance))).thenReturn(1);
        stubFinalizedAllocationChain(student, 300L, 800L, "40000.00", "40000.00");
        when(billingScheduleRepository.findByAcademicYearIdAndTermType(2L, TermType.ODD))
            .thenReturn(Optional.of(billingScheduleWithDueDate("2026-12-31")));
        when(enrollmentService.generateEnrollmentsForTermInstance(3L)).thenReturn(0);

        StudentTermEnrollment enrollment = new StudentTermEnrollment();
        enrollment.setId(950L);
        when(enrollmentRepository.findByStudentIdAndTermInstanceId(20L, 3L)).thenReturn(Optional.of(enrollment));
        // A demand already exists from a prior run -- must not be touched or re-saved.
        FeeDemand existing = new FeeDemand();
        existing.setId(960L);
        when(feeDemandRepository.findByStudentTermEnrollmentId(950L)).thenReturn(Optional.of(existing));

        LegacyFeeDemandBackfillApplyResult result = service.applyBackfill(3L);

        assertThat(result.demandsCreated()).isZero();
        verify(feeDemandRepository, never()).save(any());
    }

    @Test
    void auditFutureTermOverridesCoversEveryRemainingTermThroughProgramEnd() {
        // TERM_BASED_PROGRAM has durationYears=4 -> totalTerms=8. Student currently at semester 7
        // (year 4) must get rows for both remaining terms: 7 and 8.
        Student student = activeStudent(60L, "Senior", TERM_BASED_PROGRAM);
        when(studentRepository.findByStatus(StudentStatus.ACTIVE)).thenReturn(List.of(student));
        when(enrollmentService.computeSemesterNumber(any(), eq(termInstance))).thenReturn(7);

        StudentFeeAllocation allocation = finalizedAllocation(student, 400L);
        when(allocationRepository.findByStudentId(60L)).thenReturn(Optional.of(allocation));

        SemesterFee term7Fee = new SemesterFee(allocation, 4, "Year 4 - First Semester",
            new BigDecimal("90000.00"), LocalDate.of(2026, 12, 31), 1);
        term7Fee.setId(700L);
        when(semesterFeeRepository.findByAllocationIdAndYearNumberAndSemesterSequence(400L, 4, 1))
            .thenReturn(Optional.of(term7Fee));

        SemesterFee term8Fee = new SemesterFee(allocation, 4, "Year 4 - Second Semester",
            new BigDecimal("95000.00"), LocalDate.of(2027, 6, 30), 2);
        term8Fee.setId(701L);
        when(semesterFeeRepository.findByAllocationIdAndYearNumberAndSemesterSequence(400L, 4, 2))
            .thenReturn(Optional.of(term8Fee));

        LegacyTermOverrideSummary summary = service.auditFutureTermOverrides(3L);

        assertThat(summary.totalTermsEvaluated()).isEqualTo(2);
        assertThat(summary.okCount()).isEqualTo(2);
        assertThat(summary.rows().get(0).semesterNumber()).isEqualTo(7);
        assertThat(summary.rows().get(0).legacyAmount()).isEqualByComparingTo("90000.00");
        assertThat(summary.rows().get(1).semesterNumber()).isEqualTo(8);
        assertThat(summary.rows().get(1).legacyAmount()).isEqualByComparingTo("95000.00");
    }

    @Test
    void auditFutureTermOverridesFlagsATermWithNoMatchingLegacyRow() {
        Student student = activeStudent(61L, "GapYear", TERM_BASED_PROGRAM);
        when(studentRepository.findByStatus(StudentStatus.ACTIVE)).thenReturn(List.of(student));
        when(enrollmentService.computeSemesterNumber(any(), eq(termInstance))).thenReturn(7);

        StudentFeeAllocation allocation = finalizedAllocation(student, 401L);
        when(allocationRepository.findByStudentId(61L)).thenReturn(Optional.of(allocation));
        // Neither term 7 nor term 8's SemesterFee row is stubbed -> both resolve empty by default.

        LegacyTermOverrideSummary summary = service.auditFutureTermOverrides(3L);

        assertThat(summary.okCount()).isZero();
        assertThat(summary.exceptionCount()).isEqualTo(2);
        assertThat(summary.rows()).allMatch(r -> !r.ok() && r.exceptionReason() != null);
    }

    @Test
    void applyFutureTermOverridesWritesOneOverridePerOkTermAndReportsExceptionsWithoutWriting() {
        Student student = activeStudent(62L, "Senior", TERM_BASED_PROGRAM);
        when(studentRepository.findByStatus(StudentStatus.ACTIVE)).thenReturn(List.of(student));
        when(enrollmentService.computeSemesterNumber(any(), eq(termInstance))).thenReturn(7);

        StudentFeeAllocation allocation = finalizedAllocation(student, 402L);
        when(allocationRepository.findByStudentId(62L)).thenReturn(Optional.of(allocation));

        SemesterFee term7Fee = new SemesterFee(allocation, 4, "Year 4 - First Semester",
            new BigDecimal("90000.00"), LocalDate.of(2026, 12, 31), 1);
        term7Fee.setId(702L);
        when(semesterFeeRepository.findByAllocationIdAndYearNumberAndSemesterSequence(402L, 4, 1))
            .thenReturn(Optional.of(term7Fee));
        // Term 8 (year 4, sequence 2) left unstubbed -> exception, must not be written.

        when(currentUserResolver.resolve()).thenReturn("devadmin");
        when(termFeeOverrideRepository.findByStudentIdAndSemesterNumber(62L, 7)).thenReturn(Optional.empty());
        when(termFeeOverrideRepository.save(any(StudentTermFeeOverride.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        LegacyTermOverrideApplyResult result = service.applyFutureTermOverrides(3L);

        assertThat(result.overridesWritten()).isEqualTo(1);
        assertThat(result.exceptionDetails()).hasSize(1);
        assertThat(result.exceptionDetails().get(0)).contains("term 8");

        ArgumentCaptor<StudentTermFeeOverride> captor = ArgumentCaptor.forClass(StudentTermFeeOverride.class);
        verify(termFeeOverrideRepository).save(captor.capture());
        assertThat(captor.getValue().getSemesterNumber()).isEqualTo(7);
        assertThat(captor.getValue().getOverrideAmount()).isEqualByComparingTo("90000.00");
    }

    @Test
    void applyFutureTermOverridesOverwritesAnExistingOverrideWithTheLegacyAmount() {
        // The requirement is explicit: the originally-allocated fee structure always wins,
        // regardless of any live fee-structure change -- including overwriting a stale override
        // this same operation (or an earlier manual edit) may have left behind.
        Student student = activeStudent(63L, "Resynced", TERM_BASED_PROGRAM);
        when(studentRepository.findByStatus(StudentStatus.ACTIVE)).thenReturn(List.of(student));
        when(enrollmentService.computeSemesterNumber(any(), eq(termInstance))).thenReturn(7);

        StudentFeeAllocation allocation = finalizedAllocation(student, 403L);
        when(allocationRepository.findByStudentId(63L)).thenReturn(Optional.of(allocation));

        SemesterFee term7Fee = new SemesterFee(allocation, 4, "Year 4 - First Semester",
            new BigDecimal("90000.00"), LocalDate.of(2026, 12, 31), 1);
        term7Fee.setId(703L);
        when(semesterFeeRepository.findByAllocationIdAndYearNumberAndSemesterSequence(403L, 4, 1))
            .thenReturn(Optional.of(term7Fee));
        SemesterFee term8Fee = new SemesterFee(allocation, 4, "Year 4 - Second Semester",
            new BigDecimal("95000.00"), LocalDate.of(2027, 6, 30), 2);
        term8Fee.setId(704L);
        when(semesterFeeRepository.findByAllocationIdAndYearNumberAndSemesterSequence(403L, 4, 2))
            .thenReturn(Optional.of(term8Fee));

        StudentTermFeeOverride stale = new StudentTermFeeOverride();
        stale.setOverrideAmount(new BigDecimal("210000.00"));
        when(termFeeOverrideRepository.findByStudentIdAndSemesterNumber(63L, 7)).thenReturn(Optional.of(stale));
        when(termFeeOverrideRepository.findByStudentIdAndSemesterNumber(63L, 8)).thenReturn(Optional.empty());
        when(currentUserResolver.resolve()).thenReturn("devadmin");
        when(termFeeOverrideRepository.save(any(StudentTermFeeOverride.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        LegacyTermOverrideApplyResult result = service.applyFutureTermOverrides(3L);

        assertThat(result.overridesWritten()).isEqualTo(2);
        verify(termFeeOverrideRepository, org.mockito.Mockito.times(2)).save(any(StudentTermFeeOverride.class));
        // The pre-existing row for term 7 is reused in place (not duplicated) and overwritten
        // with the legacy amount -- proves overwrite, not skip-if-present.
        assertThat(stale.getOverrideAmount()).isEqualByComparingTo("90000.00");
        assertThat(stale.getSemesterNumber()).isEqualTo(7);
    }

    // ── Fixtures ────────────────────────────────────────────────────────────

    private static final Program TERM_BASED_PROGRAM = program(AssessmentPattern.TERM_BASED);
    private static final Program YEARLY_PROGRAM = program(AssessmentPattern.YEARLY);

    private static Program program(AssessmentPattern pattern) {
        Program p = new Program("BSc Nursing", "BSC", 4, com.cms.model.enums.ProgramStatus.ACTIVE, pattern);
        p.setId(1L);
        return p;
    }

    private Student activeStudent(Long id, String firstName, Program program) {
        Course course = new Course();
        course.setId(1L);
        course.setProgram(program);

        Cohort cohort = new Cohort();
        cohort.setId(id * 10);
        cohort.setCourse(course);

        Student student = new Student();
        student.setId(id);
        student.setFirstName(firstName);
        student.setLastName("");
        student.setCohort(cohort);
        return student;
    }

    private StudentFeeAllocation finalizedAllocation(Student student, Long allocationId) {
        StudentFeeAllocation allocation = new StudentFeeAllocation();
        allocation.setId(allocationId);
        allocation.setStatus(FeeAllocationStatus.FINALIZED);
        return allocation;
    }

    private void stubFinalizedAllocationChain(Student student, Long allocationId, Long semesterFeeId,
                                               String amount, String paid) {
        StudentFeeAllocation allocation = finalizedAllocation(student, allocationId);
        lenient().when(allocationRepository.findByStudentId(student.getId())).thenReturn(Optional.of(allocation));

        SemesterFee semesterFee = new SemesterFee(allocation, 1, "Year 1 - First Semester",
            new BigDecimal(amount), LocalDate.of(2026, 12, 31), 1);
        semesterFee.setId(semesterFeeId);
        lenient().when(semesterFeeRepository.findByAllocationIdAndYearNumberAndSemesterSequence(allocationId, 1, 1))
            .thenReturn(Optional.of(semesterFee));
        lenient().when(feeInstallmentRepository.sumAmountPaidBySemesterFeeId(semesterFeeId))
            .thenReturn(new BigDecimal(paid));
    }

    private void stubEnrollment(Long studentId, Long enrollmentId) {
        StudentTermEnrollment enrollment = new StudentTermEnrollment();
        enrollment.setId(enrollmentId);
        lenient().when(enrollmentRepository.findByStudentIdAndTermInstanceId(studentId, 3L))
            .thenReturn(Optional.of(enrollment));
        lenient().when(feeDemandRepository.save(any(FeeDemand.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private TermBillingSchedule billingScheduleWithDueDate(String isoDate) {
        TermBillingSchedule schedule = new TermBillingSchedule();
        schedule.setDueDate(LocalDate.parse(isoDate));
        return schedule;
    }

    private static FeeDemand findByEnrollmentId(List<FeeDemand> demands, Long enrollmentId) {
        return demands.stream()
            .filter(d -> d.getStudentTermEnrollment() != null && d.getStudentTermEnrollment().getId().equals(enrollmentId))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No saved FeeDemand for enrollment " + enrollmentId));
    }
}
