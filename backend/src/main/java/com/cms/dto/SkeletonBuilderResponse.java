package com.cms.dto;

import java.util.List;

/** R3.1 cohort-wide shape — one response now covers every non-elective subject a cohort has in a
 *  term, so cross-subject placement conflicts are visible in a single grid instead of hidden
 *  behind a per-subject filter. {@code subjects} carries each subject's own budget rows;
 *  {@code cells} and {@code batches} are merged across all of them, each still tagged with its
 *  owning {@code courseOfferingId}. {@code sections} lists the cohort's active Cohort Room
 *  Allocation sections for this term (empty if none committed) — reuses {@link
 *  CohortSectionResponse} directly rather than a duplicate shape, matching how {@code batches}
 *  already reuses {@link BatchDto}. {@code weeksInTerm} and {@code workingSaturdayCount} are term-
 *  wide constants (not per-subject) the frontend needs to compute an honest scheduled-hours total
 *  itself from {@code cells} — a cell placed on Monday-Friday recurs {@code weeksInTerm} times,
 *  one placed on Saturday only recurs {@code workingSaturdayCount} times (the term's real count of
 *  Saturdays matching its opt-in working-Saturday pattern, 0 if none is configured). */
public record SkeletonBuilderResponse(
    Long cohortId,
    String cohortName,
    String termInstanceLabel,
    List<SkeletonSubjectResponse> subjects,
    List<SkeletonCellResponse> cells,
    List<BatchDto> batches,
    List<CohortSectionResponse> sections,
    int weeksInTerm,
    long workingSaturdayCount
) {}
