package com.cms.dto;

import com.cms.model.enums.ClassSessionType;

/**
 * One row of "how much of this subject's curriculum hours has been placed so far". THEORY is either
 * one whole-cohort row (batchId/batchName and cohortSectionId/cohortSectionLabel all null) when the
 * cohort has no committed Cohort Room Allocation, or one row per active {@code CohortSection} when
 * it does — mirroring how LAB and CLINICAL already get one row per Batch (each batch/section needs
 * its own full quota, since they run in parallel rather than sharing sessions). The two id/label
 * pairs are kept separate rather than overloaded onto batchId/batchName since they're semantically
 * distinct occupant types.
 *
 * <p>The row is planned against the term's total hours, not a weekly count (2026-09-15):
 * {@code requiredTermRuns} is how many session occurrences the curriculum hours need across the
 * term, and {@code deliveredTermRuns} is what the placed sessions really run — a weekday session
 * runs every week, a working-Saturday session only on the chosen Saturdays. The row is met once
 * delivered reaches required. {@code requiredSessionsPerWeek}/{@code placedSessionsPerWeek} stay as
 * the familiar weekly figures for display.
 */
public record SkeletonSubjectBudget(
    ClassSessionType sessionType,
    Long batchId,
    String batchName,
    Long cohortSectionId,
    String cohortSectionLabel,
    int totalHours,
    int weeksInTerm,
    int requiredSessionsPerWeek,
    int placedSessionsPerWeek,
    int requiredTermRuns,
    int deliveredTermRuns,
    double deliveredHours
) {

    /** A row whose placed sessions all run every week — the term-wide figures follow directly from
     *  the weekly ones. */
    public SkeletonSubjectBudget(ClassSessionType sessionType, Long batchId, String batchName, Long cohortSectionId,
                                 String cohortSectionLabel, int totalHours, int weeksInTerm,
                                 int requiredSessionsPerWeek, int placedSessionsPerWeek) {
        this(sessionType, batchId, batchName, cohortSectionId, cohortSectionLabel, totalHours, weeksInTerm,
            requiredSessionsPerWeek, placedSessionsPerWeek,
            requiredSessionsPerWeek * weeksInTerm, placedSessionsPerWeek * weeksInTerm,
            requiredSessionsPerWeek > 0 ? (double) totalHours * placedSessionsPerWeek / requiredSessionsPerWeek : 0);
    }

    /** Session occurrences still owed across the term (0 once met). */
    public int remainingTermRuns() {
        return Math.max(0, requiredTermRuns - deliveredTermRuns);
    }

    public boolean isMet() {
        return deliveredTermRuns >= requiredTermRuns;
    }
}
