package com.cms.dto;

import com.cms.model.enums.ClassSessionType;

/**
 * One row of "how many weekly sessions does this many curriculum hours need, and how many have
 * been placed so far" — one row for THEORY (batchId/batchName null, whole cohort), one row per
 * Batch for LAB and CLINICAL (each batch needs its own full quota, since batches run in parallel
 * rather than sharing sessions).
 */
public record SkeletonSubjectBudget(
    ClassSessionType sessionType,
    Long batchId,
    String batchName,
    int totalHours,
    int weeksInTerm,
    int requiredSessionsPerWeek,
    int placedSessionsPerWeek
) {}
