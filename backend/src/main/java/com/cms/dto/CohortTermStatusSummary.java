package com.cms.dto;

/**
 * One row of Timetable Builder's cohort status table: this cohort's aggregate position in the
 * Pending -&gt; Draft/Generated -&gt; Conflicts Resolved -&gt; Published lifecycle for a term
 * instance, synthesized fresh on every read from its {@code ClassSchedule} rows and the same gate
 * checks {@code TimetableGenerationService#approve} itself uses -- never a persisted value.
 */
public record CohortTermStatusSummary(
    Long cohortId,
    String cohortName,
    String courseName,
    String admissionYearName,
    /** {@code "PENDING"} (no sessions placed yet) | {@code "DRAFTED"} (sessions exist but
     *  at least one of {@code approve()}'s preflight gates -- staffing, offering-assignment,
     *  coverage, conflict scan, a fresh per-cohort conflict acknowledgment -- still fails) |
     *  {@code "CONFLICTS_RESOLVED"} (every gate currently passes; ready to publish) |
     *  {@code "PUBLISHED"} | {@code "PARTIALLY_PUBLISHED"} (a post-publish edit -- Staff Session
     *  Swap, an individual Skeleton Builder placement -- created new DRAFT rows alongside
     *  already-PUBLISHED ones for this cohort/term). Drives both the status badge and the row
     *  action button on Timetable Builder's cohort table. Set by {@link
     *  com.cms.service.TimetableGenerationService#getCohortTermStatusSummaryWithReadiness}. */
    String status,
    int draftCount,
    int publishedCount,
    /** Curriculum-required THEORY/LAB/CLINICAL hours not yet placed as real sessions for this
     *  cohort/term (0 means fully covered) -- the same figure that gates Publish, computed via
     *  {@link com.cms.service.TimetableCoverageCalculator#computeCoverage}. */
    double unassignedHours,
    /** True once attendance has been recorded against any of this cohort's sessions for this term
     *  -- {@code lab_attendances.lab_schedule_id} has no ON DELETE/status-transition handling, so
     *  Discard and Revert-to-Draft both permanently refuse once this is true. The row table hides
     *  those actions instead of offering a button that can only ever fail. */
    boolean attendanceRecorded
) {}
