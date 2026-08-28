package com.cms.dto;

import java.util.List;

/** One faculty's full term workload standing, for every active faculty member regardless of
 *  whether they're anywhere near a capacity problem — unlike {@link FacultyOverCapacity}/{@link
 *  FacultyTightCapacity}, which only ever surface the faculty already in trouble. Exists so an
 *  admin can also see the opposite failure mode (a faculty sitting well under their configured
 *  capacity before assuming the institution is short-staffed) in the same place, before ever
 *  running the auto-scheduler. {@code plannedDailyHoursOverride} is the raw, editable per-faculty
 *  override (null means "falls through to the designation/system default") — the same field
 *  {@code PATCH /faculty/{id}/daily-cap} (Faculty Detail's "Raise Cap") already edits, so this
 *  view and that action can never disagree on what's actually configured. */
public record FacultyWorkloadOverviewRow(
    Long facultyId,
    String facultyName,
    String designationName,
    Integer plannedDailyHoursOverride,
    boolean capacityConfigured,
    double effectiveDailyCapacityHours,
    String dailyCapacityTier,
    int workingDaysInTerm,
    double termCapacityHours,
    double totalTermDemandHours,
    /** 0 when capacity isn't configured at all (never divide-by-zero'd into an over/tight flag). */
    double utilizationPercent,
    double shortfallHours,
    double spareHours,
    boolean overCapacity,
    boolean tightCapacity,
    List<OverageContributor> contributors
) {}
