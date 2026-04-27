package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.cms.dto.TermFeePaymentDto;
import com.cms.dto.TermFeePaymentRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.FeeDemand;
import com.cms.model.FeeStructure;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.TermBillingSchedule;
import com.cms.model.TermFeePayment;
import com.cms.model.TermInstance;
import com.cms.model.enums.DemandStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.FeeType;
import com.cms.model.enums.LateFeeType;
import com.cms.model.enums.PaymentMode;
import com.cms.model.enums.ProgramStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.FeeDemandRepository;
import com.cms.repository.TermBillingScheduleRepository;
import com.cms.repository.TermFeePaymentRepository;

@ExtendWith(MockitoExtension.class)
class TermFeePaymentServiceImplTest {

    @Mock
    private TermFeePaymentRepository paymentRepository;
    @Mock
    private FeeDemandRepository feeDemandRepository;
    @Mock
    private TermBillingScheduleRepository billingScheduleRepository;

    private TermFeePaymentServiceImpl service;

    private AcademicYear academicYear;
    private TermInstance termInstance;
    private Program program;
    private Cohort cohort;
    private Student student;
    private StudentTermEnrollment enrollment;
    private FeeDemand demand;
    private TermBillingSchedule billingSchedule;

    @BeforeEach
    void setUp() {
        service = new TermFeePaymentServiceImpl(
            paymentRepository, feeDemandRepository, billingScheduleRepository
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
    void shouldRecordPaymentWithNoLateFee() {
        LocalDate paymentDate = LocalDate.of(2026, 7, 30); // before due date
        TermFeePaymentRequest request = new TermFeePaymentRequest(
            800L, paymentDate, new BigDecimal("50000.00"), PaymentMode.CASH, "Full payment"
        );

        when(feeDemandRepository.findById(800L)).thenReturn(Optional.of(demand));
        when(billingScheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(billingSchedule));
        when(paymentRepository.countByPaymentDate(paymentDate)).thenReturn(0L);
        when(paymentRepository.save(any(TermFeePayment.class))).thenAnswer(inv -> {
            TermFeePayment p = inv.getArgument(0);
            p.setId(999L);
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            return p;
        });
        when(feeDemandRepository.save(any(FeeDemand.class))).thenReturn(demand);

        TermFeePaymentDto dto = service.recordPayment(request);

        assertThat(dto.lateFeeApplied()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.amountPaid()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(dto.demandStatus()).isEqualTo(DemandStatus.PAID);
        assertThat(dto.receiptNumber()).startsWith("RCP-20260730-");
    }

    @Test
    void shouldApplyFlatLateFeeWhenPaymentIsLate() {
        LocalDate paymentDate = LocalDate.of(2026, 8, 15); // after due + grace
        TermFeePaymentRequest request = new TermFeePaymentRequest(
            800L, paymentDate, new BigDecimal("50000.00"), PaymentMode.CASH, null
        );

        when(feeDemandRepository.findById(800L)).thenReturn(Optional.of(demand));
        when(billingScheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(billingSchedule));
        when(paymentRepository.countByPaymentDate(paymentDate)).thenReturn(0L);
        when(paymentRepository.save(any(TermFeePayment.class))).thenAnswer(inv -> {
            TermFeePayment p = inv.getArgument(0);
            p.setId(999L);
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            return p;
        });
        when(feeDemandRepository.save(any(FeeDemand.class))).thenReturn(demand);

        TermFeePaymentDto dto = service.recordPayment(request);

        // late fee = flat 500 (due=Jul 31, grace=7 → effective Jul 31+7=Aug 7, payment Aug 15 > Aug 7)
        assertThat(dto.lateFeeApplied()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void shouldApplyPerDayLateFee() {
        billingSchedule = new TermBillingSchedule(academicYear, TermType.ODD,
            LocalDate.of(2026, 7, 31), LateFeeType.PER_DAY,
            new BigDecimal("100.00"), 0);
        billingSchedule.setId(51L);

        LocalDate paymentDate = LocalDate.of(2026, 8, 5); // 5 days late
        TermFeePaymentRequest request = new TermFeePaymentRequest(
            800L, paymentDate, new BigDecimal("10000.00"), PaymentMode.UPI, null
        );

        when(feeDemandRepository.findById(800L)).thenReturn(Optional.of(demand));
        when(billingScheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(billingSchedule));
        when(paymentRepository.countByPaymentDate(paymentDate)).thenReturn(2L);
        when(paymentRepository.save(any(TermFeePayment.class))).thenAnswer(inv -> {
            TermFeePayment p = inv.getArgument(0);
            p.setId(1001L);
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            return p;
        });
        when(feeDemandRepository.save(any(FeeDemand.class))).thenReturn(demand);

        TermFeePaymentDto dto = service.recordPayment(request);

        // 5 days late * 100/day = 500
        assertThat(dto.lateFeeApplied()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(dto.receiptNumber()).endsWith("-0003"); // count=2, so next=3
    }

    @Test
    void shouldSetDemandStatusToPartialWhenPartialPayment() {
        demand.setTotalAmount(new BigDecimal("50000.00"));
        LocalDate paymentDate = LocalDate.of(2026, 7, 20);
        TermFeePaymentRequest request = new TermFeePaymentRequest(
            800L, paymentDate, new BigDecimal("20000.00"), PaymentMode.CHEQUE, null
        );

        when(feeDemandRepository.findById(800L)).thenReturn(Optional.of(demand));
        when(billingScheduleRepository.findByAcademicYearIdAndTermType(1L, TermType.ODD))
            .thenReturn(Optional.of(billingSchedule));
        when(paymentRepository.countByPaymentDate(paymentDate)).thenReturn(0L);
        when(paymentRepository.save(any(TermFeePayment.class))).thenAnswer(inv -> {
            TermFeePayment p = inv.getArgument(0);
            p.setId(1002L);
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            return p;
        });
        when(feeDemandRepository.save(any(FeeDemand.class))).thenReturn(demand);

        TermFeePaymentDto dto = service.recordPayment(request);

        assertThat(dto.demandStatus()).isEqualTo(DemandStatus.PARTIAL);
        ArgumentCaptor<FeeDemand> captor = ArgumentCaptor.forClass(FeeDemand.class);
        verify(feeDemandRepository).save(captor.capture());
        assertThat(captor.getValue().getPaidAmount())
            .isEqualByComparingTo(new BigDecimal("20000.00"));
    }

    @Test
    void shouldThrowWhenRecordingPaymentOnPaidDemand() {
        demand.setStatus(DemandStatus.PAID);
        when(feeDemandRepository.findById(800L)).thenReturn(Optional.of(demand));

        TermFeePaymentRequest request = new TermFeePaymentRequest(
            800L, LocalDate.now(), new BigDecimal("1000.00"), PaymentMode.CASH, null
        );

        assertThatThrownBy(() -> service.recordPayment(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already fully paid");
    }

    @Test
    void shouldThrowWhenDemandNotFoundForPayment() {
        when(feeDemandRepository.findById(999L)).thenReturn(Optional.empty());

        TermFeePaymentRequest request = new TermFeePaymentRequest(
            999L, LocalDate.now(), new BigDecimal("1000.00"), PaymentMode.CASH, null
        );

        assertThatThrownBy(() -> service.recordPayment(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldGetPaymentsByDemand() {
        TermFeePayment payment = buildSamplePayment(1L);
        when(feeDemandRepository.existsById(800L)).thenReturn(true);
        when(paymentRepository.findByFeeDemandId(800L)).thenReturn(List.of(payment));

        List<TermFeePaymentDto> result = service.getPaymentsByDemand(800L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).receiptNumber()).isEqualTo("RCP-20260730-0001");
    }

    @Test
    void shouldThrowWhenGettingPaymentsByDemandNotFound() {
        when(feeDemandRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.getPaymentsByDemand(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldGetPaymentByReceipt() {
        TermFeePayment payment = buildSamplePayment(1L);
        when(paymentRepository.findByReceiptNumber("RCP-20260730-0001"))
            .thenReturn(Optional.of(payment));

        TermFeePaymentDto dto = service.getPaymentByReceipt("RCP-20260730-0001");

        assertThat(dto.receiptNumber()).isEqualTo("RCP-20260730-0001");
    }

    @Test
    void shouldThrowWhenReceiptNotFound() {
        when(paymentRepository.findByReceiptNumber("INVALID"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPaymentByReceipt("INVALID"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("INVALID");
    }

    @Test
    void shouldGetById() {
        TermFeePayment payment = buildSamplePayment(1L);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        TermFeePaymentDto dto = service.getById(1L);

        assertThat(dto.id()).isEqualTo(1L);
    }

    @Test
    void shouldThrowWhenGetByIdNotFound() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void shouldGetPaymentsByDateRange() {
        TermFeePayment payment = buildSamplePayment(1L);
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);
        when(paymentRepository.findByPaymentDateBetween(from, to))
            .thenReturn(List.of(payment));

        List<TermFeePaymentDto> result = service.getPaymentsByDateRange(from, to);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldGetPaymentsWithLateFeeByTermInstance() {
        TermFeePayment paymentWithLateFee = buildSamplePayment(1L);
        paymentWithLateFee.setLateFeeApplied(new BigDecimal("500.00"));

        TermFeePayment paymentNoLateFee = buildSamplePayment(2L);
        paymentNoLateFee.setLateFeeApplied(BigDecimal.ZERO);

        when(feeDemandRepository.findByTermInstanceId(10L)).thenReturn(List.of(demand));
        when(paymentRepository.findByFeeDemandId(800L))
            .thenReturn(List.of(paymentWithLateFee, paymentNoLateFee));

        List<TermFeePaymentDto> result = service.getPaymentsWithLateFeeByTermInstance(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).lateFeeApplied()).isGreaterThan(BigDecimal.ZERO);
    }

    private TermFeePayment buildSamplePayment(Long id) {
        TermFeePayment payment = new TermFeePayment();
        payment.setId(id);
        payment.setFeeDemand(demand);
        payment.setPaymentDate(LocalDate.of(2026, 7, 30));
        payment.setAmountPaid(new BigDecimal("50000.00"));
        payment.setLateFeeApplied(BigDecimal.ZERO);
        payment.setPaymentMode(PaymentMode.CASH);
        payment.setReceiptNumber("RCP-20260730-0001");
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());
        return payment;
    }
}
