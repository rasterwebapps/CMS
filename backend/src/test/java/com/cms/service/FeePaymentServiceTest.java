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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.FeePaymentRequest;
import com.cms.dto.FeePaymentResponse;
import com.cms.dto.StudentFeeStatusResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.FeePayment;
import com.cms.model.FeeStructure;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.enums.FeeType;
import com.cms.model.enums.PaymentMode;
import com.cms.model.enums.PaymentStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.FeePaymentRepository;
import com.cms.repository.FeeStructureRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class FeePaymentServiceTest {

    @Mock
    private FeePaymentRepository feePaymentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private FeeStructureRepository feeStructureRepository;

    private FeePaymentService feePaymentService;

    private Student testStudent;
    private Program testProgram;
    private AcademicYear testAcademicYear;
    private FeeStructure testFeeStructure;

    @BeforeEach
    void setUp() {
        feePaymentService = new FeePaymentService(feePaymentRepository, studentRepository, feeStructureRepository);

        testProgram = new Program();
        testProgram.setId(1L);
        testProgram.setName("B.Tech CS");

        testAcademicYear = new AcademicYear();
        testAcademicYear.setId(1L);
        testAcademicYear.setName("2024-25");

        testStudent = new Student(
            "CS2024001", "John", "Doe", "john@college.edu",
            testProgram, 1, LocalDate.of(2024, 6, 1), StudentStatus.ACTIVE
        );
        testStudent.setId(1L);

        testFeeStructure = new FeeStructure(testProgram, testAcademicYear, FeeType.TUITION, new BigDecimal("50000.00"), true, true);
        testFeeStructure.setId(1L);
    }

    @Test
    void shouldCreateFeePayment() {
        FeePaymentRequest request = new FeePaymentRequest(
            1L, 1L, new BigDecimal("25000.00"), LocalDate.now(),
            PaymentMode.UPI, PaymentStatus.PARTIAL, "TXN123", null
        );

        FeePayment saved = createFeePayment(1L, testStudent, testFeeStructure, new BigDecimal("25000.00"), PaymentStatus.PARTIAL);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(feeStructureRepository.findById(1L)).thenReturn(Optional.of(testFeeStructure));
        when(feePaymentRepository.save(any(FeePayment.class))).thenReturn(saved);

        FeePaymentResponse response = feePaymentService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.amountPaid()).isEqualTo(new BigDecimal("25000.00"));
    }

    @Test
    void shouldThrowExceptionWhenStudentNotFound() {
        FeePaymentRequest request = new FeePaymentRequest(
            999L, 1L, new BigDecimal("25000.00"), LocalDate.now(),
            PaymentMode.UPI, PaymentStatus.PARTIAL, null, null
        );

        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feePaymentService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Student not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenFeeStructureNotFoundOnCreate() {
        FeePaymentRequest request = new FeePaymentRequest(
            1L, 999L, new BigDecimal("25000.00"), LocalDate.now(),
            PaymentMode.UPI, PaymentStatus.PARTIAL, null, null
        );

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(feeStructureRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feePaymentService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Fee structure not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenNotFoundById() {
        when(feePaymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feePaymentService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Fee payment not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenStudentNotFoundOnFindByStudentId() {
        when(studentRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> feePaymentService.findByStudentId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Student not found with id: 999");
    }

    @Test
    void shouldThrowExceptionWhenReceiptNotFound() {
        when(feePaymentRepository.findByReceiptNumber("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feePaymentService.findByReceiptNumber("INVALID"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Fee payment not found with receipt number: INVALID");
    }

    @Test
    void shouldThrowExceptionWhenStudentNotFoundOnGetFeeStatus() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feePaymentService.getStudentFeeStatus(999L, 1L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Student not found with id: 999");
    }

    @Test
    void shouldFindByDateRange() {
        FeePayment fp = createFeePayment(1L, testStudent, testFeeStructure, new BigDecimal("25000.00"), PaymentStatus.PARTIAL);
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        when(feePaymentRepository.findByPaymentDateBetween(startDate, endDate)).thenReturn(List.of(fp));

        List<FeePaymentResponse> responses = feePaymentService.findByDateRange(startDate, endDate);

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldFindAllPayments() {
        FeePayment fp = createFeePayment(1L, testStudent, testFeeStructure, new BigDecimal("25000.00"), PaymentStatus.PARTIAL);

        when(feePaymentRepository.findAll()).thenReturn(List.of(fp));

        List<FeePaymentResponse> responses = feePaymentService.findAll();

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldFindById() {
        FeePayment fp = createFeePayment(1L, testStudent, testFeeStructure, new BigDecimal("25000.00"), PaymentStatus.PARTIAL);

        when(feePaymentRepository.findById(1L)).thenReturn(Optional.of(fp));

        FeePaymentResponse response = feePaymentService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void shouldFindByStudentId() {
        FeePayment fp = createFeePayment(1L, testStudent, testFeeStructure, new BigDecimal("25000.00"), PaymentStatus.PARTIAL);

        when(studentRepository.existsById(1L)).thenReturn(true);
        when(feePaymentRepository.findByStudentId(1L)).thenReturn(List.of(fp));

        List<FeePaymentResponse> responses = feePaymentService.findByStudentId(1L);

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldFindByReceiptNumber() {
        FeePayment fp = createFeePayment(1L, testStudent, testFeeStructure, new BigDecimal("25000.00"), PaymentStatus.PARTIAL);
        fp.setReceiptNumber("RCP-123");

        when(feePaymentRepository.findByReceiptNumber("RCP-123")).thenReturn(Optional.of(fp));

        FeePaymentResponse response = feePaymentService.findByReceiptNumber("RCP-123");

        assertThat(response.receiptNumber()).isEqualTo("RCP-123");
    }

    @Test
    void shouldGetStudentFeeStatusWithNoPaidAmount() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(feeStructureRepository.findByProgramIdAndAcademicYearIdAndIsActiveTrue(1L, 1L))
            .thenReturn(List.of(testFeeStructure));
        when(feePaymentRepository.sumAmountPaidByStudentIdAndFeeStructureId(1L, 1L))
            .thenReturn(null); // No payments

        StudentFeeStatusResponse status = feePaymentService.getStudentFeeStatus(1L, 1L);

        assertThat(status.totalPaid()).isEqualTo(BigDecimal.ZERO);
        assertThat(status.feeItems().get(0).status()).isEqualTo("PENDING");
    }

    @Test
    void shouldGetStudentFeeStatus() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(feeStructureRepository.findByProgramIdAndAcademicYearIdAndIsActiveTrue(1L, 1L))
            .thenReturn(List.of(testFeeStructure));
        when(feePaymentRepository.sumAmountPaidByStudentIdAndFeeStructureId(1L, 1L))
            .thenReturn(new BigDecimal("30000.00"));

        StudentFeeStatusResponse status = feePaymentService.getStudentFeeStatus(1L, 1L);

        assertThat(status.totalFees()).isEqualTo(new BigDecimal("50000.00"));
        assertThat(status.totalPaid()).isEqualTo(new BigDecimal("30000.00"));
        assertThat(status.pendingAmount()).isEqualTo(new BigDecimal("20000.00"));
    }

    @Test
    void shouldUpdateFeePayment() {
        FeePayment existing = createFeePayment(1L, testStudent, testFeeStructure, new BigDecimal("25000.00"), PaymentStatus.PARTIAL);

        FeePaymentRequest updateRequest = new FeePaymentRequest(
            1L, 1L, new BigDecimal("50000.00"), LocalDate.now(),
            PaymentMode.CARD, PaymentStatus.PAID, "TXN456", "Full payment"
        );

        FeePayment updated = createFeePayment(1L, testStudent, testFeeStructure, new BigDecimal("50000.00"), PaymentStatus.PAID);

        when(feePaymentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(feeStructureRepository.findById(1L)).thenReturn(Optional.of(testFeeStructure));
        when(feePaymentRepository.save(any(FeePayment.class))).thenReturn(updated);

        FeePaymentResponse response = feePaymentService.update(1L, updateRequest);

        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void shouldDeleteFeePayment() {
        when(feePaymentRepository.existsById(1L)).thenReturn(true);

        feePaymentService.delete(1L);

        verify(feePaymentRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistent() {
        when(feePaymentRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> feePaymentService.delete(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Fee payment not found with id: 999");

        verify(feePaymentRepository, never()).deleteById(any());
    }

    private FeePayment createFeePayment(Long id, Student student, FeeStructure feeStructure,
                                         BigDecimal amount, PaymentStatus status) {
        FeePayment fp = new FeePayment(student, feeStructure, "RCP-" + id, amount, LocalDate.now(), PaymentMode.UPI, status);
        fp.setId(id);
        Instant now = Instant.now();
        fp.setCreatedAt(now);
        fp.setUpdatedAt(now);
        return fp;
    }
}
