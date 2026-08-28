package com.cms.dto;

import java.util.List;

/** Read-only result of {@code TimetableGlobalAutoScheduleService.precheckCapacity} — non-empty
 *  {@code overCapacityFaculty} means the global run must not be attempted at all until every
 *  listed faculty is resolved (raise their cap or spread their load), since any single faculty
 *  over capacity blocks placement for every cohort in the term, not just their own.
 *  {@code tightCapacityFaculty} is a softer, non-blocking warning — these faculty are NOT over
 *  capacity (a run may proceed once acknowledged), but their real day/period packing isn't
 *  guaranteed to succeed at ~100% utilization; see {@link FacultyTightCapacity}. */
public record GlobalCapacityPrecheckResult(
    List<FacultyOverCapacity> overCapacityFaculty,
    List<FacultyTightCapacity> tightCapacityFaculty
) {}
