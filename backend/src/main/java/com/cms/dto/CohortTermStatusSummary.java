package com.cms.dto;

/**
 * One row of Draft Review's landing summary: a cohort's aggregate publish status for a term
 * instance, synthesized from its {@code ClassSchedule} rows' {@link
 * com.cms.model.enums.ClassScheduleStatus} -- never a persisted value (ClassScheduleStatus itself
 * stays DRAFT/PUBLISHED only). {@code "PARTIALLY_PUBLISHED"} reflects the real, already-existing
 * scenario where a post-publish edit (Staff Session Swap, an individual Skeleton Builder
 * placement) creates new DRAFT rows alongside already-PUBLISHED rows for the same cohort/term.
 */
public record CohortTermStatusSummary(
    Long cohortId,
    String cohortName,
    String courseName,
    String admissionYearName,
    String status,          // "DRAFT" | "PUBLISHED" | "PARTIALLY_PUBLISHED"
    int draftCount,
    int publishedCount,
    /** Curriculum-required THEORY/LAB/CLINICAL hours not yet placed as real sessions for this
     *  cohort/term (0 means fully covered) -- the same figure that gates Publish, computed via
     *  {@link com.cms.service.TimetableCoverageCalculator#computeCoverage}. */
    double unassignedHours,
    /** OC-260: this cohort's own position in the Draft/Generated -&gt; Conflicts Resolved -&gt;
     *  Published lifecycle Skeleton Builder now drives its row action button from --
     *  {@code "DRAFT_GENERATED" | "CONFLICTS_RESOLVED" | "PUBLISHED" | "PARTIALLY_PUBLISHED"}.
     *  {@code "CONFLICTS_RESOLVED"} means every one of {@code TimetableGenerationService#approve}'s
     *  preflight gates (staffing, offering-assignment, conflict scan, coverage, and a fresh
     *  per-cohort conflict acknowledgment) currently passes for this cohort -- computed via the same
     *  gate checks so this can never say "ready" when Publish would actually still fail. Set by
     *  {@link com.cms.service.TimetableGenerationService#getCohortTermStatusSummaryWithReadiness}. */
    String readinessStatus
) {}
