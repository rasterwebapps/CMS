package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

import com.cms.dto.FeeRefundApprovalRequest;
import com.cms.dto.FeeRefundRejectionRequest;
import com.cms.dto.FeeRefundSummaryResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.FeeInstallment;
import com.cms.model.FeeRefund;
import com.cms.model.Program;
import com.cms.model.SemesterFee;
import com.cms.model.Student;
import com.cms.model.StudentFeeAllocation;
import com.cms.model.enums.FeeAllocationStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FeeInstallmentRepository;
import com.cms.repository.FeeRefundRepository;
import com.cms.repository.PaymentReceiptRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class FeeRefundServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private EnquiryRepository enquiryRepository;
    @Mock private PaymentReceiptRepository receiptRepository;
    @Mock private FeeInstallmentRepository installmentRepository;
    @Mock private EnquiryPaymentRepository enquiryPaymentRepository;
    @Mock private FeeRefundRepository refundRepository;
    @Mock private UnifiedReceiptService unifiedReceiptService;

    private FeeRefundService service;

    private Student testStudent;
    private Program testProgram;

    @BeforeEach
    void setUp() {
        service = new FeeRefundService(studentRepository, enquiryRepository, receiptRepository,
            installmentRepository, enquiryPaymentRepository, refundRepository, unifiedReceiptService);

        testProgram = new Program();
        testProgram.setId(1L);
        testProgram.setName("B.Sc CS");

        testStudent = new Student("CS2024001", "John", "Doe", "john@college.edu",
            testProgram, 1, LocalDate.of(2024, 6, 1), StudentStatus.ACTIVE);
        testStudent.setId(1L);
    }

    // ── BR-36: createAutoExcessRefund ───────────────────────────────────────────────────────

    @Test
    void shouldCreateAutoExcessRefundWithSystemSourceAndPendingStatus() {
        ArgumentCaptor<FeeRefund> captor = ArgumentCaptor.forClass(FeeRefund.class);
        when(refundRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.createAutoExcessRefund(testStudent, "RCP-2026-00001", new BigDecimal("50000"));

        FeeRefund saved = captor.getValue();
        assertThat(saved.getEntityType()).isEqualTo("STUDENT");
        assertThat(saved.getSource()).isEqualTo("AUTO_EXCESS");
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getRequestedBy()).isEqualTo("SYSTEM");
        assertThat(saved.getOriginalReceiptNumber()).isEqualTo("RCP-2026-00001");
        assertThat(saved.getStudentId()).isEqualTo(1L);
        assertThat(saved.getStudentName()).isEqualTo("John Doe");
        assertThat(saved.getRefundAmount()).isEqualByComparingTo("50000");
        assertThat(saved.getReason()).contains("50000");
    }

    // ── BR-36: rejectRefund must block AUTO_EXCESS, keep MANUAL working ────────────────────────

    @Test
    void shouldRejectAttemptToRejectAnAutoExcessRefund() {
        FeeRefund refund = new FeeRefund();
        refund.setId(10L);
        refund.setSource("AUTO_EXCESS");
        refund.setStatus("PENDING");
        when(refundRepository.findById(10L)).thenReturn(Optional.of(refund));

        FeeRefundRejectionRequest request = new FeeRefundRejectionRequest("Not a legitimate refund");

        assertThatThrownBy(() -> service.rejectRefund(10L, request, "cashier1"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Auto-generated excess refunds cannot be rejected");

        // Status must remain untouched — no save() call at all.
        verify(refundRepository, never()).save(any());
    }

    @Test
    void shouldAllowRejectingAManualRefund() {
        FeeRefund refund = new FeeRefund();
        refund.setId(11L);
        refund.setSource("MANUAL");
        refund.setStatus("PENDING");
        refund.setOriginalReceiptNumber("RCP-2026-00002");
        when(refundRepository.findById(11L)).thenReturn(Optional.of(refund));
        when(refundRepository.save(any(FeeRefund.class))).thenAnswer(inv -> inv.getArgument(0));

        FeeRefundRejectionRequest request = new FeeRefundRejectionRequest("Duplicate request");

        FeeRefundSummaryResponse response = service.rejectRefund(11L, request, "cashier1");

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(response.rejectionReason()).isEqualTo("Duplicate request");
    }

    @Test
    void shouldThrowWhenRejectingAlreadyResolvedRefund() {
        FeeRefund refund = new FeeRefund();
        refund.setId(12L);
        refund.setSource("MANUAL");
        refund.setStatus("APPROVED");
        when(refundRepository.findById(12L)).thenReturn(Optional.of(refund));

        FeeRefundRejectionRequest request = new FeeRefundRejectionRequest("Too late");

        assertThatThrownBy(() -> service.rejectRefund(12L, request, "cashier1"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Only PENDING refund requests can be rejected");
    }

    // ── BR-36: approveRefund must not soft-flag installments for AUTO_EXCESS ───────────────────

    @Test
    void shouldApproveAutoExcessRefundWithoutTouchingFeeInstallments() {
        FeeRefund refund = new FeeRefund();
        refund.setId(20L);
        refund.setEntityType("STUDENT");
        refund.setSource("AUTO_EXCESS");
        refund.setStatus("PENDING");
        refund.setOriginalReceiptNumber("RCP-2026-00003");
        refund.setStudentId(1L);
        refund.setRefundAmount(new BigDecimal("50000"));
        when(refundRepository.findById(20L)).thenReturn(Optional.of(refund));
        when(refundRepository.save(any(FeeRefund.class))).thenAnswer(inv -> inv.getArgument(0));
        when(unifiedReceiptService.generateRefundNumber(anyInt())).thenReturn("RFND-2026-0001");

        FeeRefundApprovalRequest request = new FeeRefundApprovalRequest(
            "BANK_TRANSFER", LocalDate.of(2026, 7, 20), "RETURN-TXN-1");

        FeeRefundSummaryResponse response = service.approveRefund(20L, request, "finance-head");

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.refundNumber()).isEqualTo("RFND-2026-0001");
        // The excess was never allocated to a FeeInstallment — nothing should be soft-flagged.
        verifyNoInteractions(installmentRepository);
        verifyNoInteractions(enquiryPaymentRepository);
    }

    @Test
    void shouldApproveManualStudentRefundAndSoftFlagInstallments() {
        FeeRefund refund = new FeeRefund();
        refund.setId(21L);
        refund.setEntityType("STUDENT");
        refund.setSource("MANUAL");
        refund.setStatus("PENDING");
        refund.setOriginalReceiptNumber("RCP-2026-00004");
        refund.setStudentId(1L);
        refund.setRefundAmount(new BigDecimal("50000"));

        StudentFeeAllocation allocation = new StudentFeeAllocation(
            testStudent, testProgram, new BigDecimal("400000"),
            BigDecimal.ZERO, null, BigDecimal.ZERO, new BigDecimal("400000"),
            FeeAllocationStatus.FINALIZED);
        allocation.setId(1L);
        SemesterFee semesterFee = new SemesterFee(allocation, 1, "Year 1 - Semester 1",
            new BigDecimal("200000"), LocalDate.of(2024, 7, 31), 1);
        semesterFee.setId(1L);
        FeeInstallment installment = new FeeInstallment(semesterFee, testStudent,
            new BigDecimal("50000"), LocalDate.of(2026, 4, 10),
            com.cms.model.enums.PaymentMode.UPI, "RCP-2026-00004");
        installment.setId(30L);

        when(refundRepository.findById(21L)).thenReturn(Optional.of(refund));
        when(refundRepository.save(any(FeeRefund.class))).thenAnswer(inv -> inv.getArgument(0));
        when(unifiedReceiptService.generateRefundNumber(anyInt())).thenReturn("RFND-2026-0002");
        when(installmentRepository.findByReceiptNumber("RCP-2026-00004")).thenReturn(List.of(installment));

        FeeRefundApprovalRequest request = new FeeRefundApprovalRequest(
            "CASH", LocalDate.of(2026, 7, 20), null);

        service.approveRefund(21L, request, "finance-head");

        assertThat(installment.getRefundedAt()).isNotNull();
        assertThat(installment.getRefundNumber()).isEqualTo("RFND-2026-0002");
        verify(installmentRepository).saveAll(List.of(installment));
    }

    @Test
    void shouldThrowWhenApprovingNonPendingRefund() {
        FeeRefund refund = new FeeRefund();
        refund.setId(22L);
        refund.setSource("MANUAL");
        refund.setStatus("REJECTED");
        when(refundRepository.findById(22L)).thenReturn(Optional.of(refund));

        FeeRefundApprovalRequest request = new FeeRefundApprovalRequest(
            "CASH", LocalDate.of(2026, 7, 20), null);

        assertThatThrownBy(() -> service.approveRefund(22L, request, "finance-head"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Only PENDING refund requests can be approved");
    }

    @Test
    void shouldThrowWhenRefundNotFoundOnApprove() {
        when(refundRepository.findById(999L)).thenReturn(Optional.empty());

        FeeRefundApprovalRequest request = new FeeRefundApprovalRequest(
            "CASH", LocalDate.of(2026, 7, 20), null);

        assertThatThrownBy(() -> service.approveRefund(999L, request, "finance-head"))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
