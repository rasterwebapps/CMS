package com.cms.service;

import com.cms.dto.LegacyFeeDemandBackfillApplyResult;
import com.cms.dto.LegacyFeeDemandBackfillSummary;
import com.cms.dto.LegacyTermOverrideApplyResult;
import com.cms.dto.LegacyTermOverrideSummary;

/**
 * One-time, additive billing backfill: creates {@code StudentTermEnrollment} + {@code FeeDemand}
 * rows for active students reconciled to exactly mirror their existing legacy
 * {@code StudentFeeAllocation}/{@code SemesterFee}/{@code FeeInstallment} total and paid amount
 * for the given term instance. Never modifies the legacy tables -- see the migration plan this
 * implements for why (legacy stays the system of record for Fee Explorer, Payment Collection,
 * Fee Finalization and Penalty Calculation, none of which understand FeeDemand).
 */
public interface LegacyFeeDemandBackfillService {

    /** Read-only. Safe to call repeatedly; writes nothing. */
    LegacyFeeDemandBackfillSummary auditCandidates(Long termInstanceId);

    /** Idempotent -- re-running for the same term instance only affects students not already
     *  covered by a prior run. Only acts on students the (freshly recomputed) audit marks OK. */
    LegacyFeeDemandBackfillApplyResult applyBackfill(Long termInstanceId);

    /**
     * Pins every remaining term (current term through the end of the program) for each active,
     * cohort-assigned, finalized-allocation student to their originally-allocated legacy fee --
     * never the live FeeStructureGroup guideline rate, regardless of fee-structure changes made
     * since admission. Read-only. Safe to call repeatedly; writes nothing.
     */
    LegacyTermOverrideSummary auditFutureTermOverrides(Long termInstanceId);

    /** Idempotent -- upserts one StudentTermFeeOverride per (student, term) row the audit marks
     *  OK, always setting it to the student's real legacy amount for that term. */
    LegacyTermOverrideApplyResult applyFutureTermOverrides(Long termInstanceId);
}
