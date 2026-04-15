package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.cms.dto.CollectPaymentRequest;
import com.cms.dto.CollectPaymentResponse;
import com.cms.dto.ReceiptResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.FeeInstallment;
import com.cms.model.Program;
import com.cms.model.SemesterFee;
import com.cms.model.Student;
import com.cms.model.StudentFeeAllocation;
import com.cms.model.enums.FeeAllocationStatus;
import com.cms.model.enums.PaymentMode;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.FeeInstallmentRepository;
import com.cms.repository.SemesterFeeRepository;
import com.cms.repository.StudentFeeAllocationRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentCollectionServiceTest {

    @Mock
    private StudentFeeAllocationRepository allocationRepository;
    @Mock
    private SemesterFeeRepository semesterFeeRepository;
    @Mock
    private FeeInstallmentRepository installmentRepository;
    @Mock
    private StudentRepository studentRepository;

    private PaymentCollectionService service;

    private Student testStudent;
    private Program testProgram;
    private StudentFeeAllocation testAllocation;
    private SemesterFee semesterFee1;
    private SemesterFee semesterFee2;

    @BeforeEach
    void setUp() {
        service = new PaymentCollectionService(allocationRepository, semesterFeeRepository,
            installmentRepository, studentRepository);

        testProgram = new Program();
        testProgram.setId(1L);
        testProgram.setName("B.Sc CS");

        testStudent = new Student("CS2024001", "John", "Doe", "john@college.edu",
            testProgram, 1, LocalDate.of(2024, 6, 1), StudentStatus.ACTIVE);
        testStudent.setId(1L);

        testAllocation = new StudentFeeAllocation(
            testStudent, testProgram, new BigDecimal("400000"),
            BigDecimal.ZERO, null, BigDecimal.ZERO, new BigDecimal("400000"),
            FeeAllocationStatus.FINALIZED
        );
        testAllocation.setId(1L);

        semesterFee1 = new SemesterFee(testAllocation, 1, "Year 1", new BigDecimal("200000"), LocalDate.of(2024, 7, 31));
        semesterFee1.setId(1L);

        semesterFee2 = new SemesterFee(testAllocation, 2, "Year 2", new BigDecimal("200000"), LocalDate.of(2025, 7, 31));
        semesterFee2.setId(2L);
    }

    @Test
    void shouldCollectPaymentForSingleSemester() {
        CollectPaymentRequest request = new CollectPaymentRequest(
            new BigDecimal("100000"), LocalDate.now(), PaymentMode.UPI, "TXN001", null
        );

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(testAllocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumber(1L)).thenReturn(List.of(semesterFee1, semesterFee2));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(BigDecimal.ZERO);
        when(installmentRepository.save(any(FeeInstallment.class))).thenAnswer(inv -> inv.getArgument(0));

        CollectPaymentResponse response = service.collectPayment(1L, request);

        assertThat(response.amountPaid()).isEqualTo(new BigDecimal("100000"));
        assertThat(response.studentName()).isEqualTo("John Doe");
    }

    @Test
    void shouldCarryForwardExcessPayment() {
        CollectPaymentRequest request = new CollectPaymentRequest(
            new BigDecimal("300000"), LocalDate.now(), PaymentMode.CASH, null, null
        );

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(testAllocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumber(1L)).thenReturn(List.of(semesterFee1, semesterFee2));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(BigDecimal.ZERO);
        when(installmentRepository.sumAmountPaidBySemesterFeeId(2L)).thenReturn(BigDecimal.ZERO);
        when(installmentRepository.save(any(FeeInstallment.class))).thenAnswer(inv -> inv.getArgument(0));

        CollectPaymentResponse response = service.collectPayment(1L, request);

        assertThat(response.amountPaid()).isEqualTo(new BigDecimal("300000"));
        assertThat(response.allocationSummary()).contains("Year 1");
        assertThat(response.allocationSummary()).contains("Year 2");
    }

    @Test
    void shouldSkipFullyPaidSemesters() {
        CollectPaymentRequest request = new CollectPaymentRequest(
            new BigDecimal("50000"), LocalDate.now(), PaymentMode.UPI, null, null
        );

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(testAllocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumber(1L)).thenReturn(List.of(semesterFee1, semesterFee2));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(new BigDecimal("200000")); // fully paid
        when(installmentRepository.sumAmountPaidBySemesterFeeId(2L)).thenReturn(BigDecimal.ZERO);
        when(installmentRepository.save(any(FeeInstallment.class))).thenAnswer(inv -> inv.getArgument(0));

        CollectPaymentResponse response = service.collectPayment(1L, request);

        assertThat(response.allocationSummary()).contains("Year 2");
        assertThat(response.allocationSummary()).doesNotContain("Year 1");
    }

    @Test
    void shouldThrowWhenStudentNotFound() {
        CollectPaymentRequest request = new CollectPaymentRequest(
            new BigDecimal("50000"), LocalDate.now(), PaymentMode.UPI, null, null
        );

        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.collectPayment(999L, request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowWhenAllocationNotFound() {
        CollectPaymentRequest request = new CollectPaymentRequest(
            new BigDecimal("50000"), LocalDate.now(), PaymentMode.UPI, null, null
        );

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.collectPayment(1L, request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowWhenAllocationNotFinalized() {
        testAllocation.setStatus(FeeAllocationStatus.DRAFT);
        CollectPaymentRequest request = new CollectPaymentRequest(
            new BigDecimal("50000"), LocalDate.now(), PaymentMode.UPI, null, null
        );

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(testAllocation));

        assertThatThrownBy(() -> service.collectPayment(1L, request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not finalized");
    }

    @Test
    void shouldThrowWhenNoPendingFees() {
        CollectPaymentRequest request = new CollectPaymentRequest(
            new BigDecimal("50000"), LocalDate.now(), PaymentMode.UPI, null, null
        );

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(testAllocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumber(1L)).thenReturn(List.of(semesterFee1, semesterFee2));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(new BigDecimal("200000"));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(2L)).thenReturn(new BigDecimal("200000"));

        assertThatThrownBy(() -> service.collectPayment(1L, request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No pending fees");
    }

    @Test
    void shouldGetReceipts() {
        FeeInstallment installment = new FeeInstallment(semesterFee1, testStudent,
            new BigDecimal("50000"), LocalDate.now(), PaymentMode.UPI, "RCP-001");
        installment.setId(1L);
        installment.setCreatedAt(Instant.now());

        when(studentRepository.existsById(1L)).thenReturn(true);
        when(installmentRepository.findByStudentIdOrderByPaymentDateDesc(1L)).thenReturn(List.of(installment));

        List<ReceiptResponse> receipts = service.getReceipts(1L);

        assertThat(receipts).hasSize(1);
        assertThat(receipts.get(0).receiptNumber()).isEqualTo("RCP-001");
    }

    @Test
    void shouldThrowWhenStudentNotFoundForReceipts() {
        when(studentRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.getReceipts(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldGetReceiptById() {
        FeeInstallment installment = new FeeInstallment(semesterFee1, testStudent,
            new BigDecimal("50000"), LocalDate.now(), PaymentMode.UPI, "RCP-001");
        installment.setId(1L);
        installment.setCreatedAt(Instant.now());

        when(studentRepository.existsById(1L)).thenReturn(true);
        when(installmentRepository.findById(1L)).thenReturn(Optional.of(installment));

        ReceiptResponse receipt = service.getReceiptById(1L, 1L);

        assertThat(receipt.receiptNumber()).isEqualTo("RCP-001");
    }

    @Test
    void shouldThrowWhenReceiptNotFound() {
        when(studentRepository.existsById(1L)).thenReturn(true);
        when(installmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReceiptById(1L, 999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowWhenReceiptBelongsToDifferentStudent() {
        Student otherStudent = new Student("CS2024002", "Jane", "Doe", "jane@college.edu",
            testProgram, 1, LocalDate.of(2024, 6, 1), StudentStatus.ACTIVE);
        otherStudent.setId(2L);

        FeeInstallment installment = new FeeInstallment(semesterFee1, otherStudent,
            new BigDecimal("50000"), LocalDate.now(), PaymentMode.UPI, "RCP-001");
        installment.setId(1L);
        installment.setCreatedAt(Instant.now());

        when(studentRepository.existsById(1L)).thenReturn(true);
        when(installmentRepository.findById(1L)).thenReturn(Optional.of(installment));

        assertThatThrownBy(() -> service.getReceiptById(1L, 1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowWhenStudentNotFoundForReceiptById() {
        when(studentRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.getReceiptById(999L, 1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
