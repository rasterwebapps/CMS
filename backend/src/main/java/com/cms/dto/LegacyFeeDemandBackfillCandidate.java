package com.cms.dto;

import java.math.BigDecimal;

/** One active student's eligibility for the legacy-to-FeeDemand billing backfill. {@code ok=false}
 *  rows are never written to by the backfill -- {@code exceptionReason} explains why and is meant
 *  for manual admin follow-up. */
public record LegacyFeeDemandBackfillCandidate(
    Long studentId,
    String studentName,
    Long cohortId,
    Integer semesterNumber,
    Integer yearOfStudy,
    BigDecimal legacyAmount,
    BigDecimal legacyPaid,
    boolean ok,
    String exceptionReason
) {}
