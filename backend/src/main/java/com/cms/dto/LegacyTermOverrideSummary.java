package com.cms.dto;

import java.util.List;

/** Dry-run report for {@code LegacyFeeDemandBackfillService.auditFutureTermOverrides}. */
public record LegacyTermOverrideSummary(
    int totalTermsEvaluated,
    int okCount,
    int exceptionCount,
    List<LegacyTermOverrideRow> rows
) {}
