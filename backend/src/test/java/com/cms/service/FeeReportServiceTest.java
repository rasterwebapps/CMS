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
import com.cms.dto.TermFeePaymentDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.FeeDemand;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.TermFeePayment;
import com.cms.model.TermInstance;
import com.cms.model.enums.DemandStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.PaymentMode;
import com.cms.model.enums.ProgramStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.FeeDemandRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.TermFeePaymentRepository;

@ExtendWith(MockitoExtension.class)
class FeeReportServiceTest {

    @Mock
    private FeeDemandRepository feeDemandRepository;
    @Mock
    private TermFeePaymentRepository paymentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private FeeDemandService feeDemandService;

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
        service = new FeeReportService(feeDemandRepository, paymentRepository,
            studentRepository, feeDemandService);

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
    void shouldGetLateFeeCollection() {
        TermFeePayment latePayment = buildPayment(new BigDecimal("500.00"));
        when(feeDemandRepository.findByTermInstanceId(10L)).thenReturn(List.of(demand));
        when(paymentRepository.findByFeeDemandId(800L)).thenReturn(List.of(latePayment));

        List<TermFeePaymentDto> result = service.getLateFeeCollection(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).lateFeeApplied())
            .isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void shouldExcludeZeroLateFeePaymentsFromCollection() {
        TermFeePayment noLatePayment = buildPayment(BigDecimal.ZERO);
        when(feeDemandRepository.findByTermInstanceId(10L)).thenReturn(List.of(demand));
        when(paymentRepository.findByFeeDemandId(800L)).thenReturn(List.of(noLatePayment));

        List<TermFeePaymentDto> result = service.getLateFeeCollection(10L);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldGetStudentLedger() {
        TermFeePayment payment = buildPayment(BigDecimal.ZERO);
        when(studentRepository.findById(300L)).thenReturn(Optional.of(student));
        when(feeDemandRepository.findByStudentTermEnrollmentStudentId(300L))
            .thenReturn(List.of(demand));
        when(paymentRepository.findByFeeDemandId(800L)).thenReturn(List.of(payment));

        StudentFeeLedgerDto ledger = service.getStudentLedger(300L);

        assertThat(ledger.studentId()).isEqualTo(300L);
        assertThat(ledger.studentName()).isEqualTo("Alice Smith");
        assertThat(ledger.entries()).hasSize(1);
        StudentFeeLedgerDto.LedgerEntry entry = ledger.entries().get(0);
        assertThat(entry.demandId()).isEqualTo(800L);
        assertThat(entry.payments()).hasSize(1);
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

    private TermFeePayment buildPayment(BigDecimal lateFee) {
        TermFeePayment p = new TermFeePayment();
        p.setId(1000L);
        p.setFeeDemand(demand);
        p.setPaymentDate(LocalDate.of(2026, 7, 30));
        p.setAmountPaid(new BigDecimal("50000.00"));
        p.setLateFeeApplied(lateFee);
        p.setPaymentMode(PaymentMode.CASH);
        p.setReceiptNumber("RCP-20260730-0001");
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return p;
    }
}
