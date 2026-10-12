package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

    @BeforeEach
    void setUp() {
        service = new FeeFinalizationService(allocationRepository, semesterFeeRepository, installmentRepository,
            penaltyRepository, studentRepository, enquiryRepository, enquiryPaymentRepository,
            studentScholarshipService, billingScheduleRepository, academicYearRepository, termInstanceService,
            feeDemandService, objectMapper);

        StudentFeeAllocation allocation = new StudentFeeAllocation();
        allocation.setId(ALLOCATION_ID);
        when(allocationRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.of(allocation));
    }

    private SemesterFee semesterFee(Long id, int yearNumber, int sequence, BigDecimal amount) {
        SemesterFee sf = new SemesterFee();
        sf.setId(id);
        sf.setYearNumber(yearNumber);
        sf.setSemesterSequence(sequence);
        sf.setAmount(amount);
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
}
