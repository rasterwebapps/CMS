package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.TermFeeOverrideInput;
import com.cms.dto.TermFeeRow;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.Course;
import com.cms.model.FeeDemand;
import com.cms.model.FeeStructure;
import com.cms.model.FeeStructureGroup;
import com.cms.model.FeeStructureYearAmount;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.StudentTermFeeOverride;
import com.cms.model.TermInstance;
import com.cms.model.enums.AssessmentPattern;
import com.cms.model.enums.DemandStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.FeeType;
import com.cms.model.enums.StudentType;
import com.cms.model.enums.TermType;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.repository.FeeDemandRepository;
import com.cms.repository.FeeStructureGroupRepository;
import com.cms.repository.FeeStructureRepository;
import com.cms.repository.FeeStructureYearAmountRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermBillingScheduleRepository;
import com.cms.repository.TermInstanceRepository;

/**
 * Covers the boarding-status-switch fee adjustment path added to FeeDemandServiceImpl —
 * separate from the disabled legacy FeeDemandServiceImplTest (pending its own Phase 5 rewrite).
 */
@ExtendWith(MockitoExtension.class)
class FeeDemandStudentTypeSwitchTest {

    @Mock private FeeDemandRepository feeDemandRepository;
    @Mock private TermInstanceRepository termInstanceRepository;
    @Mock private StudentTermEnrollmentRepository enrollmentRepository;
    @Mock private FeeStructureGroupRepository feeStructureGroupRepository;
    @Mock private FeeStructureRepository feeStructureRepository;
    @Mock private FeeStructureYearAmountRepository yearAmountRepository;
    @Mock private TermBillingScheduleRepository billingScheduleRepository;
    @Mock private AdmissionRepository admissionRepository;
    @Mock private EnquiryPaymentRepository enquiryPaymentRepository;
    @Mock private com.cms.repository.StudentTermFeeOverrideRepository termFeeOverrideRepository;
    @Mock private com.cms.util.CurrentUserResolver currentUserResolver;

    private FeeDemandServiceImpl service;

    private static final Long PROGRAM_ID = 1L;
    private static final Long ACADEMIC_YEAR_ID = 10L;
    private static final Long GROUP_ID = 100L;
    private static final Integer YEAR_OF_STUDY = 1;

    @BeforeEach
    void setUp() {
        service = new FeeDemandServiceImpl(feeDemandRepository, termInstanceRepository, enrollmentRepository,
            feeStructureGroupRepository, feeStructureRepository, yearAmountRepository,
            billingScheduleRepository, admissionRepository, enquiryPaymentRepository,
            termFeeOverrideRepository, currentUserResolver);
    }

    @Test
    void switchingToHostelerAddsHostelFeeToUnpaidDemand() {
        FeeDemand demand = demand(1L, StudentType.DAY_SCHOLAR, DemandStatus.UNPAID,
            new BigDecimal("50000.00"), BigDecimal.ZERO);
        stubFeePlan(tuitionAndHostelStructures());
        when(feeDemandRepository.findByStudentTermEnrollmentStudentId(5L)).thenReturn(List.of(demand));

        FeeDemandService.StudentTypeSwitchImpact impact =
            service.applyStudentTypeSwitchAdjustment(5L, StudentType.HOSTELER, null);

        // 50000 tuition + 15000 hostel = 65000
        assertThat(impact.demandsAffected()).isEqualTo(1);
        assertThat(impact.totalDelta()).isEqualByComparingTo("15000.00");
        assertThat(demand.getTotalAmount()).isEqualByComparingTo("65000.00");
        assertThat(demand.getStatus()).isEqualTo(DemandStatus.UNPAID);
    }

    @Test
    void switchingToDayScholarRemovesHostelFeeAndCanFlipPartialToPaid() {
        // Already paid 60000 of a 65000 (tuition+hostel) demand — dropping hostel brings the
        // total to 50000, which the existing payment now fully covers.
        FeeDemand demand = demand(2L, StudentType.HOSTELER, DemandStatus.PARTIAL,
            new BigDecimal("65000.00"), new BigDecimal("60000.00"));
        stubFeePlan(tuitionAndHostelStructures());
        when(feeDemandRepository.findByStudentTermEnrollmentStudentId(6L)).thenReturn(List.of(demand));

        FeeDemandService.StudentTypeSwitchImpact impact =
            service.applyStudentTypeSwitchAdjustment(6L, StudentType.DAY_SCHOLAR, null);

        assertThat(impact.totalDelta()).isEqualByComparingTo("-15000.00");
        assertThat(demand.getTotalAmount()).isEqualByComparingTo("50000.00");
        assertThat(demand.getStatus()).isEqualTo(DemandStatus.PAID);
    }

    @Test
    void hostelOnlyPlanResolvesToZeroForDayScholarInsteadOfThrowing() {
        FeeDemand demand = demand(3L, StudentType.HOSTELER, DemandStatus.UNPAID,
            new BigDecimal("15000.00"), BigDecimal.ZERO);
        stubFeePlan(hostelOnlyStructure());
        when(feeDemandRepository.findByStudentTermEnrollmentStudentId(7L)).thenReturn(List.of(demand));

        FeeDemandService.StudentTypeSwitchImpact impact =
            service.applyStudentTypeSwitchAdjustment(7L, StudentType.DAY_SCHOLAR, null);

        assertThat(impact.demandsAffected()).isEqualTo(1);
        assertThat(demand.getTotalAmount()).isEqualByComparingTo("0.00");
        assertThat(demand.getStatus()).isEqualTo(DemandStatus.PAID);
    }

    @Test
    void paidAndWaivedDemandsAreNeverTouched() {
        FeeDemand paid = demand(4L, StudentType.DAY_SCHOLAR, DemandStatus.PAID,
            new BigDecimal("50000.00"), new BigDecimal("50000.00"));
        FeeDemand waived = demand(5L, StudentType.DAY_SCHOLAR, DemandStatus.WAIVED,
            new BigDecimal("50000.00"), BigDecimal.ZERO);
        when(feeDemandRepository.findByStudentTermEnrollmentStudentId(8L)).thenReturn(List.of(paid, waived));

        FeeDemandService.StudentTypeSwitchImpact impact =
            service.applyStudentTypeSwitchAdjustment(8L, StudentType.HOSTELER, null);

        assertThat(impact.demandsAffected()).isZero();
        assertThat(impact.totalDelta()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(paid.getTotalAmount()).isEqualByComparingTo("50000.00");
        assertThat(waived.getTotalAmount()).isEqualByComparingTo("50000.00");
        verify(feeDemandRepository, never()).save(any());
    }

    @Test
    void previewDoesNotPersistAnything() {
        FeeDemand demand = demand(6L, StudentType.DAY_SCHOLAR, DemandStatus.UNPAID,
            new BigDecimal("50000.00"), BigDecimal.ZERO);
        stubFeePlan(tuitionAndHostelStructures());
        when(feeDemandRepository.findByStudentTermEnrollmentStudentId(9L)).thenReturn(List.of(demand));

        FeeDemandService.StudentTypeSwitchImpact impact =
            service.previewStudentTypeSwitchImpact(9L, StudentType.HOSTELER);

        assertThat(impact.totalDelta()).isEqualByComparingTo("15000.00");
        assertThat(demand.getTotalAmount()).isEqualByComparingTo("50000.00"); // unchanged
        verify(feeDemandRepository, never()).save(any());
    }

    @Test
    void outstandingDuesSumsPositiveNonWaivedOutstandingOnly() {
        FeeDemand unpaid = demand(7L, StudentType.DAY_SCHOLAR, DemandStatus.UNPAID,
            new BigDecimal("50000.00"), new BigDecimal("20000.00")); // 30000 outstanding
        FeeDemand overpaid = demand(8L, StudentType.DAY_SCHOLAR, DemandStatus.PAID,
            new BigDecimal("40000.00"), new BigDecimal("45000.00")); // -5000, ignored
        FeeDemand waivedWithBalance = demand(9L, StudentType.DAY_SCHOLAR, DemandStatus.WAIVED,
            new BigDecimal("10000.00"), BigDecimal.ZERO); // excluded entirely
        when(feeDemandRepository.findByStudentTermEnrollmentStudentId(11L))
            .thenReturn(List.of(unpaid, overpaid, waivedWithBalance));

        BigDecimal dues = service.getOutstandingDuesForStudent(11L);

        assertThat(dues).isEqualByComparingTo("30000.00");
    }

    // ── Term fee override tests ───────────────────────────────────────────────

    @Test
    void savedOverrideWinsOverCalculatedAmountForCurrentTermDemand() {
        FeeDemand demand = demand(10L, StudentType.DAY_SCHOLAR, DemandStatus.UNPAID,
            new BigDecimal("50000.00"), BigDecimal.ZERO);
        demand.getStudentTermEnrollment().setSemesterNumber(5);
        Long enrollmentStudentId = demand.getStudentTermEnrollment().getStudent().getId();

        StudentTermFeeOverride override = new StudentTermFeeOverride();
        override.setOverrideAmount(new BigDecimal("80000.00"));
        when(termFeeOverrideRepository.findByStudentIdAndSemesterNumber(enrollmentStudentId, 5))
            .thenReturn(Optional.of(override));
        when(feeDemandRepository.findByStudentTermEnrollmentStudentId(20L)).thenReturn(List.of(demand));

        // No fee-plan stubbing at all -- proves the override short-circuits before the
        // fee-structure lookup is ever reached.
        FeeDemandService.StudentTypeSwitchImpact impact =
            service.applyStudentTypeSwitchAdjustment(20L, StudentType.HOSTELER, null);

        assertThat(demand.getTotalAmount()).isEqualByComparingTo("80000.00");
        assertThat(impact.totalDelta()).isEqualByComparingTo("30000.00");
    }

    @Test
    void applyPersistsSubmittedOverridesBeforeRecomputingDemands() {
        Student student = new Student();
        student.setId(30L);
        StudentTermEnrollment currentEnrollment = new StudentTermEnrollment();
        currentEnrollment.setStudent(student);
        currentEnrollment.setSemesterNumber(5);

        when(enrollmentRepository.findByStudentIdAndStatus(30L, EnrollmentStatus.ENROLLED))
            .thenReturn(Optional.of(currentEnrollment));
        when(feeDemandRepository.findByStudentTermEnrollmentStudentId(30L)).thenReturn(List.of());
        when(currentUserResolver.resolve()).thenReturn("admin");
        when(termFeeOverrideRepository.findByStudentIdAndSemesterNumber(30L, 6))
            .thenReturn(Optional.empty());

        service.applyStudentTypeSwitchAdjustment(30L, StudentType.HOSTELER,
            List.of(new TermFeeOverrideInput(6, new BigDecimal("80000.00"))));

        ArgumentCaptor<StudentTermFeeOverride> captor = ArgumentCaptor.forClass(StudentTermFeeOverride.class);
        verify(termFeeOverrideRepository).save(captor.capture());
        assertThat(captor.getValue().getSemesterNumber()).isEqualTo(6);
        assertThat(captor.getValue().getOverrideAmount()).isEqualByComparingTo("80000.00");
        assertThat(captor.getValue().getSetBy()).isEqualTo("admin");
        assertThat(captor.getValue().getStudent()).isSameAs(student);
    }

    @Test
    void applyUpdatesExistingOverrideInsteadOfDuplicating() {
        Student student = new Student();
        student.setId(31L);
        StudentTermEnrollment currentEnrollment = new StudentTermEnrollment();
        currentEnrollment.setStudent(student);
        currentEnrollment.setSemesterNumber(5);

        StudentTermFeeOverride existing = new StudentTermFeeOverride();
        existing.setSemesterNumber(5);
        existing.setOverrideAmount(new BigDecimal("80000.00"));

        when(enrollmentRepository.findByStudentIdAndStatus(31L, EnrollmentStatus.ENROLLED))
            .thenReturn(Optional.of(currentEnrollment));
        when(feeDemandRepository.findByStudentTermEnrollmentStudentId(31L)).thenReturn(List.of());
        when(currentUserResolver.resolve()).thenReturn("admin");
        when(termFeeOverrideRepository.findByStudentIdAndSemesterNumber(31L, 5))
            .thenReturn(Optional.of(existing));

        service.applyStudentTypeSwitchAdjustment(31L, StudentType.DAY_SCHOLAR,
            List.of(new TermFeeOverrideInput(5, new BigDecimal("70000.00"))));

        ArgumentCaptor<StudentTermFeeOverride> captor = ArgumentCaptor.forClass(StudentTermFeeOverride.class);
        verify(termFeeOverrideRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        assertThat(captor.getValue().getOverrideAmount()).isEqualByComparingTo("70000.00");
    }

    @Test
    void previewTermFeeScheduleNullsCalculatedAmountWhenNoFeePlanConfiguredAndSurfacesExistingOverride() {
        Program program = new Program();
        program.setId(PROGRAM_ID);
        program.setAssessmentPattern(AssessmentPattern.YEARLY);
        program.setDurationYears(2);

        Course course = new Course();
        course.setProgram(program);
        Cohort cohort = new Cohort();
        cohort.setCourse(course);

        AcademicYear academicYear = new AcademicYear();
        academicYear.setId(ACADEMIC_YEAR_ID);

        TermInstance termInstance = new TermInstance();
        termInstance.setAcademicYear(academicYear);
        termInstance.setTermType(TermType.ODD);

        Student student = new Student();
        student.setId(40L);

        StudentTermEnrollment currentEnrollment = new StudentTermEnrollment();
        currentEnrollment.setStudent(student);
        currentEnrollment.setCohort(cohort);
        currentEnrollment.setSemesterNumber(1);
        currentEnrollment.setYearOfStudy(1);
        currentEnrollment.setTermInstance(termInstance);

        when(enrollmentRepository.findByStudentIdAndStatus(40L, EnrollmentStatus.ENROLLED))
            .thenReturn(Optional.of(currentEnrollment));
        stubFeePlan(tuitionAndHostelStructures()); // only configured for YEAR_OF_STUDY (1)

        when(termFeeOverrideRepository.findByStudentIdAndSemesterNumber(40L, 1)).thenReturn(Optional.empty());
        StudentTermFeeOverride override = new StudentTermFeeOverride();
        override.setOverrideAmount(new BigDecimal("80000.00"));
        when(termFeeOverrideRepository.findByStudentIdAndSemesterNumber(40L, 2))
            .thenReturn(Optional.of(override));

        List<TermFeeRow> rows = service.previewTermFeeSchedule(40L, StudentType.HOSTELER);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).semesterNumber()).isEqualTo(1);
        assertThat(rows.get(0).calculatedAmount()).isEqualByComparingTo("65000.00");
        assertThat(rows.get(0).existingOverride()).isNull();
        assertThat(rows.get(1).semesterNumber()).isEqualTo(2);
        assertThat(rows.get(1).calculatedAmount()).isNull(); // no year-2 plan configured yet
        assertThat(rows.get(1).existingOverride()).isEqualByComparingTo("80000.00");
    }

    // ── Fixtures ────────────────────────────────────────────────────────────

    private FeeDemand demand(Long id, StudentType studentType, DemandStatus status,
                              BigDecimal totalAmount, BigDecimal paidAmount) {
        Program program = new Program();
        program.setId(PROGRAM_ID);

        Course course = new Course();
        course.setProgram(program);

        Cohort cohort = new Cohort();
        cohort.setCourse(course);

        Student student = new Student();
        student.setId(100L + id);
        student.setStudentType(studentType);

        StudentTermEnrollment enrollment = new StudentTermEnrollment();
        enrollment.setId(id);
        enrollment.setStudent(student);
        enrollment.setCohort(cohort);
        enrollment.setYearOfStudy(YEAR_OF_STUDY);

        AcademicYear academicYear = new AcademicYear();
        academicYear.setId(ACADEMIC_YEAR_ID);
        academicYear.setName("2026-27");

        TermInstance termInstance = new TermInstance();
        termInstance.setTermType(TermType.ODD);

        FeeDemand demand = new FeeDemand();
        demand.setId(id);
        demand.setStudentTermEnrollment(enrollment);
        demand.setTermInstance(termInstance);
        demand.setAcademicYear(academicYear);
        demand.setTotalAmount(totalAmount);
        demand.setPaidAmount(paidAmount);
        demand.setStatus(status);
        return demand;
    }

    private FeeStructureGroup group() {
        FeeStructureGroup group = new FeeStructureGroup();
        return group;
    }

    private List<FeeStructure> tuitionAndHostelStructures() {
        FeeStructureGroup group = group();
        FeeStructure tuition = new FeeStructure(group, FeeType.TUITION, new BigDecimal("50000.00"), true, true);
        tuition.setId(1L);
        FeeStructure hostel = new FeeStructure(group, FeeType.HOSTEL_FEE, new BigDecimal("15000.00"), true, true);
        hostel.setId(2L);

        lenient().when(yearAmountRepository.findByFeeStructureIdAndYearNumber(1L, YEAR_OF_STUDY))
            .thenReturn(List.of(new FeeStructureYearAmount(tuition, YEAR_OF_STUDY, "Year 1", new BigDecimal("50000.00"))));
        lenient().when(yearAmountRepository.findByFeeStructureIdAndYearNumber(2L, YEAR_OF_STUDY))
            .thenReturn(List.of(new FeeStructureYearAmount(hostel, YEAR_OF_STUDY, "Year 1", new BigDecimal("15000.00"))));

        return List.of(tuition, hostel);
    }

    private List<FeeStructure> hostelOnlyStructure() {
        FeeStructureGroup group = group();
        FeeStructure hostel = new FeeStructure(group, FeeType.HOSTEL_FEE, new BigDecimal("15000.00"), true, true);
        hostel.setId(3L);

        lenient().when(yearAmountRepository.findByFeeStructureIdAndYearNumber(3L, YEAR_OF_STUDY))
            .thenReturn(List.of(new FeeStructureYearAmount(hostel, YEAR_OF_STUDY, "Year 1", new BigDecimal("15000.00"))));

        return List.of(hostel);
    }

    private void stubFeePlan(List<FeeStructure> structures) {
        FeeStructureGroup group = structures.get(0).getFeeStructureGroup();
        // group.getId() is null on a bare `new FeeStructureGroup()`, which is fine — the mocked
        // repositories below are stubbed by argument matchers, not by that id.
        when(feeStructureGroupRepository.findByProgramIdAndAcademicYearId(anyLong(), anyLong()))
            .thenReturn(List.of(group));
        when(feeStructureRepository.findByFeeStructureGroupIdAndIsActiveTrue(any()))
            .thenReturn(structures);
    }
}
