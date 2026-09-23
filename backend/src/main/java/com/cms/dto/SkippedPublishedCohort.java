package com.cms.dto;

/** One cohort a {@code TimetableGlobalAutoScheduleService.runGlobalAutoSchedule} "All Cohorts" run
 *  deliberately left untouched because THIS cohort's own timetable is already approved/{@code
 *  PUBLISHED} — once approved on Draft Review, only manual period/staff edits (swap staff, swap
 *  sessions) are allowed, never a full automated re-run, so this cohort is excluded from the batch
 *  entirely rather than silently skipped with no trace. Approve is cohort-scoped (OC-258/OC-260), so
 *  other cohorts sharing the same term instance may still be placed normally in the same run. */
public record SkippedPublishedCohort(
    Long cohortId,
    String cohortName
) {}
