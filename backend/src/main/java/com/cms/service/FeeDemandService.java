package com.cms.service;

import java.math.BigDecimal;
import java.util.List;

import com.cms.dto.FeeDemandDto;
import com.cms.dto.TermFeeOverrideInput;
import com.cms.dto.TermFeeRow;
import com.cms.model.enums.DemandStatus;
import com.cms.model.enums.StudentType;

public interface FeeDemandService {

    /** Result of a demand generation run. */
    record GenerateResult(int demandsCreated, int yearlySkipped) {}

    /** One demand's total-amount recomputation as part of a boarding-status switch. */
    record DemandAdjustment(
        Long demandId,
        Long enrollmentId,
        String termLabel,
        BigDecimal previousAmount,
        BigDecimal newAmount,
        BigDecimal delta
    ) {}

    /** Aggregate effect of switching a student's studentType on their not-yet-fully-paid demands. */
    record StudentTypeSwitchImpact(
        int demandsAffected,
        BigDecimal totalDelta,
        List<DemandAdjustment> adjustments
    ) {}

    /**
     * Generates fee demands for all ENROLLED students in the given term instance.
     * Idempotent — skips students who already have a demand. Yearly-pattern students
     * are skipped on EVEN terms (annual fee is billed at ODD term opening).
     */
    GenerateResult generateDemandsForTermInstance(Long termInstanceId);

    List<FeeDemandDto> getDemandsByTermInstance(Long termInstanceId);

    List<FeeDemandDto> getDemandsByTermInstanceAndStatus(Long termInstanceId, DemandStatus status);

    FeeDemandDto getDemandByEnrollment(Long enrollmentId);

    FeeDemandDto getById(Long id);

    List<FeeDemandDto> getOutstandingDemands(Long termInstanceId);

    List<FeeDemandDto> getDemandsByStudent(Long studentId);

    /**
     * Dry-run: recomputes what each of the student's not-yet-fully-paid demands (status
     * UNPAID/PARTIAL) would total under {@code targetType}, without persisting anything.
     */
    StudentTypeSwitchImpact previewStudentTypeSwitchImpact(Long studentId, StudentType targetType);

    /**
     * Recomputes and saves the new totalAmount (and resulting status) for the student's
     * not-yet-fully-paid demands under {@code targetType}. Already-PAID/WAIVED demands are left
     * untouched — no retroactive refund is issued; any resulting credit is handled manually.
     * {@code overrides} (sparse -- only terms the admin edited) are persisted first, so the
     * current term's recomputation above and every later {@link #generateDemandsForTermInstance}
     * run both pick them up.
     */
    StudentTypeSwitchImpact applyStudentTypeSwitchAdjustment(Long studentId, StudentType targetType,
                                                              List<TermFeeOverrideInput> overrides);

    /**
     * The current term plus every remaining term through the student's program length, each with
     * its calculated amount under {@code targetType} (null if no fee structure is configured yet
     * for that term's year of study) and any previously saved override.
     */
    List<TermFeeRow> previewTermFeeSchedule(Long studentId, StudentType targetType);

    /** Sum of outstanding (unpaid) amounts across the student's non-WAIVED demands. */
    BigDecimal getOutstandingDuesForStudent(Long studentId);
}
