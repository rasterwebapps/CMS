package com.cms.dto;

import java.util.List;

/** Outcome of running {@code LegacyFeeDemandBackfillService.applyBackfill} for one term instance.
 *  {@code exceptionStudents} lists every student excluded from this run (by id/name/reason) so
 *  none are silently dropped -- they need separate manual follow-up. */
public record LegacyFeeDemandBackfillApplyResult(
    int enrollmentsCreated,
    int demandsCreated,
    int reconciled,
    List<String> exceptionStudents
) {}
