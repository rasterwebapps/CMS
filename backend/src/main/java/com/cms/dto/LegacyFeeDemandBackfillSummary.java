package com.cms.dto;

import java.util.List;

/** Dry-run report for {@code LegacyFeeDemandBackfillService.auditCandidates} -- reviewed by an
 *  admin before {@code applyBackfill} is ever called for the same term instance. */
public record LegacyFeeDemandBackfillSummary(
    int totalActiveStudents,
    int okCount,
    int exceptionCount,
    List<LegacyFeeDemandBackfillCandidate> candidates
) {}
