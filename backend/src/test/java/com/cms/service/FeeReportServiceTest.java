package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.FeeCollectionSummaryDto;
import com.cms.dto.StudentFeeLedgerDto;
import com.cms.dto.YearFeeFromEnquiry;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.Course;
import com.cms.model.FeeDemand;
import com.cms.model.Program;
import com.cms.model.SemesterFee;
import com.cms.model.Student;
import com.cms.model.StudentFeeAllocation;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.TermInstance;
import com.cms.model.enums.DemandStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.FeeAllocationStatus;
import com.cms.model.enums.ProgramStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FeeDemandRepository;
import com.cms.repository.FeeInstallmentRepository;
import com.cms.repository.PaymentReceiptRepository;
import com.cms.repository.SemesterFeeRepository;
import com.cms.repository.StudentFeeAllocationRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class FeeReportServiceTest {

    @Mock
    private FeeDemandRepository feeDemandRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private FeeDemandService feeDemandService;
    @Mock
    private StudentFeeAllocationRepository allocationRepository;
    @Mock
    private SemesterFeeRepository semesterFeeRepository;
    @Mock
    private FeeInstallmentRepository installmentRepository;
    @Mock
    private EnquiryRepository enquiryRepository;
    @Mock
    private EnquiryPaymentRepository enquiryPaymentRepository;
    @Mock
    private FeeFinalizationService feeFinalizationService;
    @Mock
    private PaymentReceiptRepository paymentReceiptRepository;

    private FeeReportService service;

    private AcademicYear academicYear;
    private TermInstance termInstance;
    private Program program;
    private Cohort cohort;
    private Student student;
    private StudentTermEnrollment enrollment;
    private FeeDemand demand;

    @BeforeEach
    void setUp() {
        service = new FeeReportService(feeDemandRepository,
            studentRepository, feeDemandService, allocationRepository, semesterFeeRepository,
            installmentRepository, enquiryRepository, enquiryPaymentRepository, feeFinalizationService,
            paymentReceiptRepository);

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

        Course course = new Course("BSc Nursing", "BSCN", null, program);
        course.setId(101L);

        cohort = new Cohort();
        cohort.setId(200L);
        cohort.setCourse(course);
        cohort.setCohortCode("BSCN-2026-2030");

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

        demand = new FeeDemand();
        demand.setId(800L);
        demand.setStudentTermEnrollment(enrollment);
        demand.setTermInstance(termInstance);
        demand.setAcademicYear(academicYear);
        demand.setTotalAmount(new BigDecimal("50000.00"));
        demand.setDueDate(LocalDate.of(2026, 7, 31));
        demand.setPaidAmount(BigDecimal.ZERO);
        demand.setStatus(DemandStatus.UNPAID);
        demand.setCreatedAt(Instant.now());
        demand.setUpdatedAt(Instant.now());
    }

    @Test
    void shouldGetCollectionSummary() {
        when(feeDemandRepository.findByTermInstanceId(10L)).thenReturn(List.of(demand));

        List<FeeCollectionSummaryDto> result = service.getCollectionSummary(10L);

        assertThat(result).hasSize(1);
        FeeCollectionSummaryDto summary = result.get(0);
        assertThat(summary.programName()).isEqualTo("BSc Nursing");
        assertThat(summary.programCode()).isEqualTo("BSCN");
        assertThat(summary.totalDemands()).isEqualTo(1);
        assertThat(summary.totalAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(summary.collectedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.unpaidCount()).isEqualTo(1);
        assertThat(summary.paidCount()).isEqualTo(0);
        assertThat(summary.partialCount()).isEqualTo(0);
    }

    @Test
    void shouldGetCollectionSummaryWithPaidAndPartial() {
        FeeDemand paidDemand = buildDemand(DemandStatus.PAID, new BigDecimal("50000.00"),
            new BigDecimal("50000.00"));
        FeeDemand partialDemand = buildDemand(DemandStatus.PARTIAL, new BigDecimal("50000.00"),
            new BigDecimal("25000.00"));

        when(feeDemandRepository.findByTermInstanceId(10L))
            .thenReturn(List.of(demand, paidDemand, partialDemand));

        List<FeeCollectionSummaryDto> result = service.getCollectionSummary(10L);

        assertThat(result).hasSize(1);
        FeeCollectionSummaryDto summary = result.get(0);
        assertThat(summary.totalDemands()).isEqualTo(3);
        assertThat(summary.paidCount()).isEqualTo(1);
        assertThat(summary.partialCount()).isEqualTo(1);
        assertThat(summary.unpaidCount()).isEqualTo(1);
    }

    @Test
    void shouldGetStudentLedger() {
        when(studentRepository.findById(300L)).thenReturn(Optional.of(student));
        when(allocationRepository.findByStudentId(300L)).thenReturn(Optional.empty());
        when(feeDemandRepository.findByStudentTermEnrollmentStudentId(300L))
            .thenReturn(List.of(demand));

        StudentFeeLedgerDto ledger = service.getStudentLedger(300L);

        assertThat(ledger.studentId()).isEqualTo(300L);
        assertThat(ledger.studentName()).isEqualTo("Alice Smith");
        assertThat(ledger.entries()).hasSize(1);
        StudentFeeLedgerDto.LedgerEntry entry = ledger.entries().get(0);
        assertThat(entry.demandId()).isEqualTo(800L);
        assertThat(entry.payments()).isEmpty();
    }

    @Test
    void shouldPreferStudentFeeAllocationLedger() {
        StudentFeeAllocation allocation = new StudentFeeAllocation(
            student, program, new BigDecimal("50000.00"), BigDecimal.ZERO, null,
            BigDecimal.ZERO, new BigDecimal("50000.00"), FeeAllocationStatus.FINALIZED
        );
        allocation.setId(700L);
        SemesterFee semesterFee = new SemesterFee(
            allocation, 1, "Year 1 - First Installment", new BigDecimal("25000.00"),
            LocalDate.of(2026, 7, 31), 1
        );
        semesterFee.setId(701L);

        when(studentRepository.findById(300L)).thenReturn(Optional.of(student));
        when(allocationRepository.findByStudentId(300L)).thenReturn(Optional.of(allocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(700L))
            .thenReturn(List.of(semesterFee));
        when(enquiryRepository.findByConvertedStudentId(300L)).thenReturn(Optional.empty());
        when(installmentRepository.sumAmountPaidBySemesterFeeId(701L)).thenReturn(BigDecimal.ZERO);
        when(installmentRepository.findBySemesterFeeId(701L)).thenReturn(List.of());

        StudentFeeLedgerDto ledger = service.getStudentLedger(300L);

        assertThat(ledger.entries()).hasSize(1);
        StudentFeeLedgerDto.LedgerEntry entry = ledger.entries().get(0);
        assertThat(entry.demandId()).isEqualTo(701L);
        assertThat(entry.termLabel()).isEqualTo("Year 1 - First Installment");
        assertThat(entry.totalAmount()).isEqualByComparingTo(new BigDecimal("25000.00"));
        assertThat(entry.status()).isEqualTo(DemandStatus.UNPAID);
    }

    @Test
    void shouldBuildLedgerFromEnquiryYearFeesWhenAllocationAndDemandsAreMissing() {
        when(studentRepository.findById(300L)).thenReturn(Optional.of(student));
        when(allocationRepository.findByStudentId(300L)).thenReturn(Optional.empty());
        when(feeDemandRepository.findByStudentTermEnrollmentStudentId(300L)).thenReturn(List.of());
        when(feeFinalizationService.getEnquiryYearFees(300L)).thenReturn(List.of(
            new YearFeeFromEnquiry(1, new BigDecimal("50000.00"), LocalDate.of(2026, 7, 31))
        ));
        when(enquiryRepository.findByConvertedStudentId(300L)).thenReturn(Optional.empty());

        StudentFeeLedgerDto ledger = service.getStudentLedger(300L);

        assertThat(ledger.entries()).hasSize(2);
        assertThat(ledger.entries().get(0).termLabel()).isEqualTo("Year 1 - First Installment");
        assertThat(ledger.entries().get(0).totalAmount()).isEqualByComparingTo(new BigDecimal("25000.00"));
        assertThat(ledger.entries().get(1).termLabel()).isEqualTo("Year 1 - Second Installment");
        assertThat(ledger.entries().get(1).dueDate()).isEqualTo(LocalDate.of(2027, 1, 31));
    }

    @Test
    void shouldThrowWhenStudentNotFoundForLedger() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStudentLedger(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    private FeeDemand buildDemand(DemandStatus status, BigDecimal total, BigDecimal paid) {
        FeeDemand d = new FeeDemand();
        d.setId(900L + status.ordinal());
        d.setStudentTermEnrollment(enrollment);
        d.setTermInstance(termInstance);
        d.setAcademicYear(academicYear);
        d.setTotalAmount(total);
        d.setDueDate(LocalDate.of(2026, 7, 31));
        d.setPaidAmount(paid);
        d.setStatus(status);
        d.setCreatedAt(Instant.now());
        d.setUpdatedAt(Instant.now());
        return d;
    }
}
