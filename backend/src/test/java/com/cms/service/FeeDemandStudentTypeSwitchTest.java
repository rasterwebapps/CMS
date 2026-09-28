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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.cms.model.TermInstance;
import com.cms.model.enums.DemandStatus;
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

    private FeeDemandServiceImpl service;

    private static final Long PROGRAM_ID = 1L;
    private static final Long ACADEMIC_YEAR_ID = 10L;
    private static final Long GROUP_ID = 100L;
    private static final Integer YEAR_OF_STUDY = 1;

    @BeforeEach
    void setUp() {
        service = new FeeDemandServiceImpl(feeDemandRepository, termInstanceRepository, enrollmentRepository,
            feeStructureGroupRepository, feeStructureRepository, yearAmountRepository,
            billingScheduleRepository, admissionRepository, enquiryPaymentRepository);
    }

    @Test
    void switchingToHostelerAddsHostelFeeToUnpaidDemand() {
        FeeDemand demand = demand(1L, StudentType.DAY_SCHOLAR, DemandStatus.UNPAID,
            new BigDecimal("50000.00"), BigDecimal.ZERO);
        stubFeePlan(tuitionAndHostelStructures());
        when(feeDemandRepository.findByStudentTermEnrollmentStudentId(5L)).thenReturn(List.of(demand));

        FeeDemandService.StudentTypeSwitchImpact impact =
            service.applyStudentTypeSwitchAdjustment(5L, StudentType.HOSTELER);

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
            service.applyStudentTypeSwitchAdjustment(6L, StudentType.DAY_SCHOLAR);

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
            service.applyStudentTypeSwitchAdjustment(7L, StudentType.DAY_SCHOLAR);

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
            service.applyStudentTypeSwitchAdjustment(8L, StudentType.HOSTELER);

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
