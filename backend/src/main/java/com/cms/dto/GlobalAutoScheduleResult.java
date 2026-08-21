package com.cms.dto;

import java.util.List;

/** Result of {@code TimetableGlobalAutoScheduleService.runGlobalAutoSchedule} — only ever returned
 *  on full success, since any single unplaceable session aborts the whole run (see
 *  {@code TimetableConstraintViolationException} instead). */
public record GlobalAutoScheduleResult(
    int totalPlaced,
    int totalStaffed,
    List<CohortPlacementSummary> cohortSummaries
) {}
