package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.model.SemesterFee;
import com.cms.model.StudentFeeAllocation;
import com.cms.model.enums.StudentType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FeeInstallmentRepository;
import com.cms.repository.PenaltyRepository;
import com.cms.repository.SemesterFeeRepository;
import com.cms.repository.StudentFeeAllocationRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.TermBillingScheduleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Covers {@link FeeFinalizationService#syncSemesterFeesForStudentTypeSwitch} -- the fix for the
 * boarding-status switch silently leaving a legacy-allocation student's Fee Explorer/installment
 * screen on its pre-switch amount, because the switch's own recompute
 * (FeeDemandServiceImpl.applyStudentTypeSwitchAdjustment) only ever touched FeeDemand rows, which
 * a student finalized under the older StudentFeeAllocation/SemesterFee system never has.
 *
 * <p>The rest of {@link FeeFinalizationService} is covered (or pending a rewrite) in
 * {@link FeeFinalizationServiceTest} -- this class is scoped narrowly to the new method so it
 * doesn't need the full BR-30 multi-dimension fee-structure mock setup.
 */
@ExtendWith(MockitoExtension.class)
class FeeFinalizationServiceLegacySyncTest {

    @Mock private StudentFeeAllocationRepository allocationRepository;
    @Mock private SemesterFeeRepository semesterFeeRepository;
    @Mock private FeeInstallmentRepository installmentRepository;
    @Mock private PenaltyRepository penaltyRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private EnquiryRepository enquiryRepository;
    @Mock private EnquiryPaymentRepository enquiryPaymentRepository;
    @Mock private StudentScholarshipService studentScholarshipService;
    @Mock private TermBillingScheduleRepository billingScheduleRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private TermInstanceService termInstanceService;
    @Mock private FeeDemandService feeDemandService;
    @Mock private ObjectMapper objectMapper;

    private FeeFinalizationService service;

    private static final Long STUDENT_ID = 42L;
    private static final Long ALLOCATION_ID = 7L;

    private StudentFeeAllocation allocation;

    @BeforeEach
    void setUp() {
        service = new FeeFinalizationService(allocationRepository, semesterFeeRepository, installmentRepository,
            penaltyRepository, studentRepository, enquiryRepository, enquiryPaymentRepository,
            studentScholarshipService, billingScheduleRepository, academicYearRepository, termInstanceService,
            feeDemandService, objectMapper);

        allocation = new StudentFeeAllocation();
        allocation.setId(ALLOCATION_ID);
        when(allocationRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.of(allocation));
    }

    private SemesterFee semesterFee(Long id, int yearNumber, int sequence, BigDecimal amount) {
        SemesterFee sf = new SemesterFee();
        sf.setId(id);
        sf.setYearNumber(yearNumber);
        sf.setSemesterSequence(sequence);
        sf.setAmount(amount);
        sf.setDueDate(LocalDate.now().plusMonths(3));
        return sf;
    }

    private SemesterFee semesterFeeDue(Long id, int yearNumber, int sequence, BigDecimal amount, LocalDate dueDate) {
        SemesterFee sf = semesterFee(id, yearNumber, sequence, amount);
        sf.setDueDate(dueDate);
        return sf;
    }

    @Test
    void noAllocation_returnsZeroAndNeverTouchesFeeDemandService() {
        when(allocationRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());

        FeeFinalizationService.LegacyFeeSyncResult result =
            service.syncSemesterFeesForStudentTypeSwitch(STUDENT_ID, StudentType.HOSTELER);

        assertThat(result.installmentsAdjusted()).isEqualTo(0);
        assertThat(result.totalDelta()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(feeDemandService, never()).resolveTermFeeAmount(any(), anyInt(), any());
    }

    @Test
    void fullyPaidInstallment_isNeverRecomputedOrSaved() {
        SemesterFee paid = semesterFee(1L, 4, 1, new BigDecimal("87500"));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(ALLOCATION_ID))
            .thenReturn(List.of(paid));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(new BigDecimal("87500"));

        FeeFinalizationService.LegacyFeeSyncResult result =
            service.syncSemesterFeesForStudentTypeSwitch(STUDENT_ID, StudentType.HOSTELER);

        assertThat(result.installmentsAdjusted()).isEqualTo(0);
        assertThat(result.totalDelta()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(feeDemandService, never()).resolveTermFeeAmount(any(), anyInt(), any());
        verify(semesterFeeRepository, never()).save(any());
    }

    @Test
    void unpaidInstallment_withNoResolvableEnrollment_isLeftUnchanged() {
        SemesterFee unpaid = semesterFee(2L, 4, 1, new BigDecimal("87500"));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(ALLOCATION_ID))
            .thenReturn(List.of(unpaid));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(2L)).thenReturn(BigDecimal.ZERO);
        // yearNumber=4, sequence=1 => global semesterNumber 7
        when(feeDemandService.resolveTermFeeAmount(STUDENT_ID, 7, StudentType.HOSTELER)).thenReturn(null);

        FeeFinalizationService.LegacyFeeSyncResult result =
            service.syncSemesterFeesForStudentTypeSwitch(STUDENT_ID, StudentType.HOSTELER);

        assertThat(result.installmentsAdjusted()).isEqualTo(0);
        assertThat(unpaid.getAmount()).isEqualByComparingTo("87500");
        verify(semesterFeeRepository, never()).save(any());
    }

    @Test
    void unpaidInstallment_withUnchangedRecomputedAmount_isNotCountedOrSaved() {
        SemesterFee unpaid = semesterFee(3L, 4, 2, new BigDecimal("87500"));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(ALLOCATION_ID))
            .thenReturn(List.of(unpaid));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(3L)).thenReturn(BigDecimal.ZERO);
        // yearNumber=4, sequence=2 => global semesterNumber 8
        when(feeDemandService.resolveTermFeeAmount(STUDENT_ID, 8, StudentType.DAY_SCHOLAR))
            .thenReturn(new BigDecimal("87500"));

        FeeFinalizationService.LegacyFeeSyncResult result =
            service.syncSemesterFeesForStudentTypeSwitch(STUDENT_ID, StudentType.DAY_SCHOLAR);

        assertThat(result.installmentsAdjusted()).isEqualTo(0);
        assertThat(result.totalDelta()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(semesterFeeRepository, never()).save(any());
    }

    @Test
    void unpaidInstallment_withChangedAmount_isUpdatedSavedAndCounted() {
        SemesterFee unpaid = semesterFee(4L, 4, 1, new BigDecimal("87500"));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(ALLOCATION_ID))
            .thenReturn(List.of(unpaid));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(4L)).thenReturn(BigDecimal.ZERO);
        when(feeDemandService.resolveTermFeeAmount(STUDENT_ID, 7, StudentType.HOSTELER))
            .thenReturn(new BigDecimal("72500"));

        FeeFinalizationService.LegacyFeeSyncResult result =
            service.syncSemesterFeesForStudentTypeSwitch(STUDENT_ID, StudentType.HOSTELER);

        assertThat(result.installmentsAdjusted()).isEqualTo(1);
        assertThat(result.totalDelta()).isEqualByComparingTo(new BigDecimal("-15000"));
        assertThat(unpaid.getAmount()).isEqualByComparingTo("72500");
        verify(semesterFeeRepository).save(unpaid);
    }

    @Test
    void partiallyPaidInstallment_isStillEligibleForRecompute() {
        SemesterFee partial = semesterFee(5L, 4, 1, new BigDecimal("87500"));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(ALLOCATION_ID))
            .thenReturn(List.of(partial));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(5L)).thenReturn(new BigDecimal("10000"));
        when(feeDemandService.resolveTermFeeAmount(STUDENT_ID, 7, StudentType.HOSTELER))
            .thenReturn(new BigDecimal("72500"));

        FeeFinalizationService.LegacyFeeSyncResult result =
            service.syncSemesterFeesForStudentTypeSwitch(STUDENT_ID, StudentType.HOSTELER);

        assertThat(result.installmentsAdjusted()).isEqualTo(1);
        assertThat(partial.getAmount()).isEqualByComparingTo("72500");
    }

    // -- collection-date cutoff + never-below-paid (Gaadha Prasad incident, 2026-10-13) ----------

    @Test
    void pastDueUnpaidInstallment_isNeverRepriced() {
        SemesterFee pastDue = semesterFeeDue(6L, 1, 2, new BigDecimal("142500"), LocalDate.now().minusDays(1));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(ALLOCATION_ID))
            .thenReturn(List.of(pastDue));

        FeeFinalizationService.LegacyFeeSyncResult result =
            service.syncSemesterFeesForStudentTypeSwitch(STUDENT_ID, StudentType.DAY_SCHOLAR);

        assertThat(result.installmentsAdjusted()).isEqualTo(0);
        assertThat(pastDue.getAmount()).isEqualByComparingTo("142500");
        verify(feeDemandService, never()).resolveTermFeeAmount(any(), anyInt(), any());
        verify(semesterFeeRepository, never()).save(any());
    }

    @Test
    void pastDuePartlyPaidInstallment_isNeverRepriced() {
        // Exact incident shape: 127,500 collected against 142,500, due date long past.
        SemesterFee pastDue = semesterFeeDue(7L, 1, 1, new BigDecimal("142500"), LocalDate.now().minusMonths(10));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(ALLOCATION_ID))
            .thenReturn(List.of(pastDue));

        service.syncSemesterFeesForStudentTypeSwitch(STUDENT_ID, StudentType.DAY_SCHOLAR);

        assertThat(pastDue.getAmount()).isEqualByComparingTo("142500");
        verify(semesterFeeRepository, never()).save(any());
    }

    @Test
    void installmentDueToday_isStillRepriced() {
        // "has not passed the collection date" -- the due date itself is still inside the window.
        SemesterFee dueToday = semesterFeeDue(8L, 2, 1, new BigDecimal("115000"), LocalDate.now());
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(ALLOCATION_ID))
            .thenReturn(List.of(dueToday));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(8L)).thenReturn(BigDecimal.ZERO);
        when(feeDemandService.resolveTermFeeAmount(STUDENT_ID, 3, StudentType.DAY_SCHOLAR))
            .thenReturn(new BigDecimal("95000"));

        FeeFinalizationService.LegacyFeeSyncResult result =
            service.syncSemesterFeesForStudentTypeSwitch(STUDENT_ID, StudentType.DAY_SCHOLAR);

        assertThat(result.installmentsAdjusted()).isEqualTo(1);
        assertThat(dueToday.getAmount()).isEqualByComparingTo("95000");
    }

    @Test
    void newAmountBelowAlreadyPaid_isSkipped() {
        SemesterFee partial = semesterFee(9L, 4, 1, new BigDecimal("87500"));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(ALLOCATION_ID))
            .thenReturn(List.of(partial));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(9L)).thenReturn(new BigDecimal("60000"));
        when(feeDemandService.resolveTermFeeAmount(STUDENT_ID, 7, StudentType.DAY_SCHOLAR))
            .thenReturn(new BigDecimal("50000"));

        FeeFinalizationService.LegacyFeeSyncResult result =
            service.syncSemesterFeesForStudentTypeSwitch(STUDENT_ID, StudentType.DAY_SCHOLAR);

        assertThat(result.installmentsAdjusted()).isEqualTo(0);
        assertThat(partial.getAmount()).isEqualByComparingTo("87500");
        verify(semesterFeeRepository, never()).save(any());
    }

    @Test
    void newAmountExactlyEqualToPaid_isAllowed() {
        SemesterFee partial = semesterFee(10L, 4, 1, new BigDecimal("87500"));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(ALLOCATION_ID))
            .thenReturn(List.of(partial));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(10L)).thenReturn(new BigDecimal("60000"));
        when(feeDemandService.resolveTermFeeAmount(STUDENT_ID, 7, StudentType.DAY_SCHOLAR))
            .thenReturn(new BigDecimal("60000"));

        FeeFinalizationService.LegacyFeeSyncResult result =
            service.syncSemesterFeesForStudentTypeSwitch(STUDENT_ID, StudentType.DAY_SCHOLAR);

        assertThat(result.installmentsAdjusted()).isEqualTo(1);
        assertThat(partial.getAmount()).isEqualByComparingTo("60000");
    }

    @Test
    void mixedPastAndFutureInstallments_onlyFutureOnesChange() {
        SemesterFee past1 = semesterFeeDue(11L, 1, 1, new BigDecimal("142500"), LocalDate.now().minusMonths(10));
        SemesterFee past2 = semesterFeeDue(12L, 1, 2, new BigDecimal("142500"), LocalDate.now().minusDays(5));
        SemesterFee future = semesterFee(13L, 2, 1, new BigDecimal("115000"));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(ALLOCATION_ID))
            .thenReturn(List.of(past1, past2, future));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(13L)).thenReturn(BigDecimal.ZERO);
        when(feeDemandService.resolveTermFeeAmount(STUDENT_ID, 3, StudentType.DAY_SCHOLAR))
            .thenReturn(new BigDecimal("95000"));

        FeeFinalizationService.LegacyFeeSyncResult result =
            service.syncSemesterFeesForStudentTypeSwitch(STUDENT_ID, StudentType.DAY_SCHOLAR);

        assertThat(result.installmentsAdjusted()).isEqualTo(1);
        assertThat(result.totalDelta()).isEqualByComparingTo(new BigDecimal("-20000"));
        assertThat(past1.getAmount()).isEqualByComparingTo("142500");
        assertThat(past2.getAmount()).isEqualByComparingTo("142500");
        assertThat(allocation.getNetFee()).isEqualByComparingTo("380000");
        assertThat(allocation.getTotalFee()).isEqualByComparingTo("380000");
        assertThat(allocation.isHasHostelFee()).isFalse();
        verify(semesterFeeRepository).save(future);
        verify(semesterFeeRepository, never()).save(past1);
        verify(semesterFeeRepository, never()).save(past2);
        verify(allocationRepository).save(allocation);
    }

    @Test
    void updatedInstallments_recomputesTotalFeePreservingDiscountAndUpdatesHasHostelFee() {
        allocation.setDiscountAmount(new BigDecimal("10000"));
        allocation.setTotalFee(new BigDecimal("210000"));
        allocation.setNetFee(new BigDecimal("200000"));
        allocation.setHasHostelFee(false);

        SemesterFee paid = semesterFeeDue(14L, 1, 1, new BigDecimal("100000"), LocalDate.now().minusMonths(6));
        SemesterFee future = semesterFee(15L, 1, 2, new BigDecimal("100000"));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(ALLOCATION_ID))
            .thenReturn(List.of(paid, future));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(15L)).thenReturn(BigDecimal.ZERO);
        when(feeDemandService.resolveTermFeeAmount(STUDENT_ID, 2, StudentType.HOSTELER))
            .thenReturn(new BigDecimal("125000"));

        FeeFinalizationService.LegacyFeeSyncResult result =
            service.syncSemesterFeesForStudentTypeSwitch(STUDENT_ID, StudentType.HOSTELER);

        assertThat(result.installmentsAdjusted()).isEqualTo(1);
        assertThat(result.totalDelta()).isEqualByComparingTo("25000");
        assertThat(allocation.getNetFee()).isEqualByComparingTo("225000");
        assertThat(allocation.getTotalFee()).isEqualByComparingTo("235000");
        assertThat(allocation.isHasHostelFee()).isTrue();
        verify(allocationRepository).save(allocation);
    }
}
