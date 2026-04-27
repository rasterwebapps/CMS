package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.FeeDemandDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.FeeDemand;
import com.cms.model.FeeStructure;
import com.cms.model.FeeStructureYearAmount;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.TermBillingSchedule;
import com.cms.model.TermInstance;
import com.cms.model.enums.CohortStatus;
import com.cms.model.enums.DemandStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.FeeType;
import com.cms.model.enums.LateFeeType;
import com.cms.model.enums.ProgramStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.FeeDemandRepository;
import com.cms.repository.FeeStructureRepository;
import com.cms.repository.FeeStructureYearAmountRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermBillingScheduleRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class FeeDemandServiceImplTest {

    @Mock
    private FeeDemandRepository feeDemandRepository;
    @Mock
    private TermInstanceRepository termInstanceRepository;
    @Mock
    private StudentTermEnrollmentRepository enrollmentRepository;
    @Mock
    private FeeStructureRepository feeStructureRepository;
    @Mock
    private FeeStructureYearAmountRepository yearAmountRepository;
    @Mock
    private TermBillingScheduleRepository billingScheduleRepository;

    private FeeDemandServiceImpl service;

    private AcademicYear academicYear;
    private TermInstance termInstance;
    private Program program;
    private Cohort cohort;
    private Student student;
    private StudentTermEnrollment enrollment;
    private TermBillingSchedule billingSchedule;
    private FeeStructure feeStructure;
    private FeeStructureYearAmount yearAmount;

    @BeforeEach
    void setUp() {
        service = new FeeDemandServiceImpl(
            feeDemandRepository,
            termInstanceRepository,
            enrollmentRepository,
            feeStructureRepository,
            yearAmountRepository,
            billingScheduleRepository
        );

        academicYear = new AcademicYear("2026-2027",
            LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), true);
        academicYear.setId(1L);
        academicYear.setCreatedAt(Instant.now());
        academicYear.setUpdatedAt(Instant.now());

        termInstance = new TermInstance(academicYear, TermType.ODD,
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 30), TermInstanceStatus.OPEN);
        termInstance.setId(10L);
        termInstance.setCreatedAt(Instant.now());
        termInstance.setUpdatedAt(Instant.now());

        program = new Program("BSc Nursing", "BSCN", 4, ProgramStatus.ACTIVE);
        program.setId(100L);

        cohort = new Cohort();
        cohort.setId(200L);
        cohort.setProgram(program);
        cohort.setCohortCode("BSCN-2026-2030");
        cohort.setStatus(CohortStatus.ACTIVE);

        student = new Student();
        student.setId(300L);
        student.setFirstName("Alice");
        student.setLastName("Smith");
        student.setEmail("alice@test.com");
        student.setProgram(program);

        enrollment = new StudentTermEnrollment();
        enrollment.setId(400L);
        enrollment.setStudent(student);
        enrollment.setTermInstance(termInstance);
        enrollment.setCohort(cohort);
        enrollment.setSemesterNumber(1);
        enrollment.setYearOfStudy(1);
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        enrollment.setCreatedAt(Instant.now());
        enrollment.setUpdatedAt(Instant.now());

        billingSchedule = new TermBillingSchedule(academicYear, TermType.ODD,
            LocalDate.of(2026, 7, 31), LateFeeType.FLAT,
            new BigDecimal("500.00"), 7);
        billingSchedule.setId(50L);

        feeStructure = new FeeStructure(program, academicYear, FeeType.TUITION,
            new BigDecimal("50000.00"), true, true);
        feeStructure.setId(500L);

        yearAmount = new FeeStructureYearAmount(feeStructure, 1, "Year 1",
            new BigDecimal("50000.00"));
        yearAmount.setId(600L);
    }

    @Test
    void shouldGenerateDemandsForOpenTermInstance() {
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(billingScheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(billingSchedule));
        when(enrollmentRepository.findByTermInstanceIdAndStatus(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(List.of(enrollment));
        when(feeDemandRepository.findByStudentTermEnrollmentId(400L))
            .thenReturn(Optional.empty());
        when(feeStructureRepository.findByProgramIdAndAcademicYearIdAndIsActiveTrue(100L, 1L))
            .thenReturn(List.of(feeStructure));
        when(yearAmountRepository.findByFeeStructureIdAndYearNumber(500L, 1))
            .thenReturn(List.of(yearAmount));
        when(feeDemandRepository.save(any(FeeDemand.class))).thenAnswer(inv -> {
            FeeDemand d = inv.getArgument(0);
            d.setId(999L);
            d.setCreatedAt(Instant.now());
            d.setUpdatedAt(Instant.now());
            return d;
        });

        int count = service.generateDemandsForTermInstance(10L);

        assertThat(count).isEqualTo(1);
        ArgumentCaptor<FeeDemand> captor = ArgumentCaptor.forClass(FeeDemand.class);
        verify(feeDemandRepository).save(captor.capture());
        FeeDemand saved = captor.getValue();
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(saved.getDueDate()).isEqualTo(LocalDate.of(2026, 7, 31));
        assertThat(saved.getStatus()).isEqualTo(DemandStatus.UNPAID);
        assertThat(saved.getPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldSkipEnrollmentsWithExistingDemand() {
        FeeDemand existing = new FeeDemand();
        existing.setId(777L);

        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(billingScheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(billingSchedule));
        when(enrollmentRepository.findByTermInstanceIdAndStatus(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(List.of(enrollment));
        when(feeDemandRepository.findByStudentTermEnrollmentId(400L))
            .thenReturn(Optional.of(existing));

        int count = service.generateDemandsForTermInstance(10L);

        assertThat(count).isEqualTo(0);
        verify(feeDemandRepository, never()).save(any(FeeDemand.class));
    }

    @Test
    void shouldThrowWhenTermInstanceNotFound() {
        when(termInstanceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateDemandsForTermInstance(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldThrowWhenTermInstanceNotOpen() {
        termInstance.setStatus(TermInstanceStatus.PLANNED);
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));

        assertThatThrownBy(() -> service.generateDemandsForTermInstance(10L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("OPEN");
    }

    @Test
    void shouldThrowWhenNoBillingScheduleConfigured() {
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(billingScheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateDemandsForTermInstance(10L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("billing schedule");
    }

    @Test
    void shouldThrowWhenNoFeePlanConfigured() {
        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        when(billingScheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(billingSchedule));
        when(enrollmentRepository.findByTermInstanceIdAndStatus(10L, EnrollmentStatus.ENROLLED))
            .thenReturn(List.of(enrollment));
        when(feeDemandRepository.findByStudentTermEnrollmentId(400L))
            .thenReturn(Optional.empty());
        when(feeStructureRepository.findByProgramIdAndAcademicYearIdAndIsActiveTrue(100L, 1L))
            .thenReturn(List.of());

        assertThatThrownBy(() -> service.generateDemandsForTermInstance(10L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No fee plan configured");
    }

    @Test
    void shouldGetDemandsByTermInstance() {
        FeeDemand demand = buildSampleDemand(1L);
        when(feeDemandRepository.findByTermInstanceId(10L)).thenReturn(List.of(demand));

        List<FeeDemandDto> result = service.getDemandsByTermInstance(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).status()).isEqualTo(DemandStatus.UNPAID);
    }

    @Test
    void shouldGetOutstandingDemands() {
        FeeDemand demand = buildSampleDemand(1L);
        when(feeDemandRepository.findByTermInstanceIdAndStatusNot(10L, DemandStatus.PAID))
            .thenReturn(List.of(demand));

        List<FeeDemandDto> result = service.getOutstandingDemands(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).outstandingAmount())
            .isEqualByComparingTo(new BigDecimal("50000.00"));
    }

    @Test
    void shouldGetDemandByEnrollment() {
        FeeDemand demand = buildSampleDemand(1L);
        when(feeDemandRepository.findByStudentTermEnrollmentId(400L))
            .thenReturn(Optional.of(demand));

        FeeDemandDto dto = service.getDemandByEnrollment(400L);

        assertThat(dto.enrollmentId()).isEqualTo(400L);
        assertThat(dto.studentName()).isEqualTo("Alice Smith");
    }

    @Test
    void shouldThrowWhenDemandByEnrollmentNotFound() {
        when(feeDemandRepository.findByStudentTermEnrollmentId(999L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDemandByEnrollment(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldGetById() {
        FeeDemand demand = buildSampleDemand(1L);
        when(feeDemandRepository.findById(1L)).thenReturn(Optional.of(demand));

        FeeDemandDto dto = service.getById(1L);

        assertThat(dto.id()).isEqualTo(1L);
    }

    @Test
    void shouldThrowWhenGetByIdNotFound() {
        when(feeDemandRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldGetDemandsByTermInstanceAndStatus() {
        FeeDemand demand = buildSampleDemand(1L);
        when(feeDemandRepository.findByTermInstanceIdAndStatus(10L, DemandStatus.UNPAID))
            .thenReturn(List.of(demand));

        List<FeeDemandDto> result = service.getDemandsByTermInstanceAndStatus(10L, DemandStatus.UNPAID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(DemandStatus.UNPAID);
    }

    @Test
    void shouldGetDemandsByStudent() {
        FeeDemand demand = buildSampleDemand(1L);
        when(feeDemandRepository.findByStudentTermEnrollmentStudentId(300L))
            .thenReturn(List.of(demand));

        List<FeeDemandDto> result = service.getDemandsByStudent(300L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).studentId()).isEqualTo(300L);
    }

    private FeeDemand buildSampleDemand(Long id) {
        FeeDemand demand = new FeeDemand();
        demand.setId(id);
        demand.setStudentTermEnrollment(enrollment);
        demand.setTermInstance(termInstance);
        demand.setAcademicYear(academicYear);
        demand.setTotalAmount(new BigDecimal("50000.00"));
        demand.setDueDate(LocalDate.of(2026, 7, 31));
        demand.setPaidAmount(BigDecimal.ZERO);
        demand.setStatus(DemandStatus.UNPAID);
        demand.setCreatedAt(Instant.now());
        demand.setUpdatedAt(Instant.now());
        return demand;
    }
}
