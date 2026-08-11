package com.cms.dto;

import com.cms.model.enums.ClassSessionType;

/**
 * One row of "how many weekly sessions does this many curriculum hours need, and how many have
 * been placed so far". THEORY is either one whole-cohort row (batchId/batchName and
 * cohortSectionId/cohortSectionLabel all null) when the cohort has no committed Cohort Room
 * Allocation, or one row per active {@code CohortSection} when it does — mirroring how LAB and
 * CLINICAL already get one row per Batch (each batch/section needs its own full quota, since they
 * run in parallel rather than sharing sessions). The two id/label pairs are kept separate rather
 * than overloaded onto batchId/batchName since they're semantically distinct occupant types.
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
    int placedSessionsPerWeek
) {}
