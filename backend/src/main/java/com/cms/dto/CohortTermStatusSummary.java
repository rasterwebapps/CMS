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
    double unassignedHours
) {}
