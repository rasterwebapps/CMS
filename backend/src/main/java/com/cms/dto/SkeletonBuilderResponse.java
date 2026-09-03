package com.cms.dto;

import java.util.List;

/** R3.1 cohort-wide shape — one response now covers every non-elective subject a cohort has in a
 *  term, so cross-subject placement conflicts are visible in a single grid instead of hidden
 *  behind a per-subject filter. {@code subjects} carries each subject's own budget rows;
 *  {@code cells} and {@code batches} are merged across all of them, each still tagged with its
 *  owning {@code courseOfferingId}. {@code sections} lists the cohort's active Cohort Room
 *  Allocation sections for this term (empty if none committed) — reuses {@link
 *  CohortSectionResponse} directly rather than a duplicate shape, matching how {@code batches}
 *  already reuses {@link BatchDto}. {@code weeksInTerm} and {@code workingSaturdayCount} are term-
 *  wide constants (not per-subject) the frontend needs to compute an honest scheduled-hours total
 *  itself from {@code cells} — a cell placed on Monday-Friday recurs {@code weeksInTerm} times,
 *  one placed on Saturday only recurs {@code workingSaturdayCount} times (the term's real count of
 *  Saturdays matching its opt-in working-Saturday pattern, 0 if none is configured). {@code
 *  clinicalShiftHours} carries Clinical hours delivered via an active Clinical Shift Group instead
 *  of a grid cell — see {@link SkeletonClinicalShiftHours}. {@code termTimetablePublished} is true
 *  once this term's timetable has been approved on Draft Review (a term-wide fact, same for every
 *  cohort in it) — past that point, Global Auto-Schedule refuses to run and only manual period/
 *  staff edits (swap staff, swap sessions) remain available; it has nothing to do with whether this
 *  cohort's Cohort Room Allocation is committed, which only gates whether {@code sections} is
 *  non-empty. {@code clinicalShiftWindows} is this cohort's active Clinical Shift wall-clock
 *  windows (bus-depart through bus-return, per day) — used by the frontend grid to widen its
 *  displayed time range and render the shift block alongside the Period columns; the actual
 *  period-level hard block is already enforced server-side (see {@code
 *  TimetableGlobalAutoScheduleService#tryPlaceAndStaff}), so this is purely a rendering aid. */
public record SkeletonBuilderResponse(
    Long cohortId,
    String cohortName,
    String termInstanceLabel,
    List<SkeletonSubjectResponse> subjects,
    List<SkeletonCellResponse> cells,
    List<BatchDto> batches,
    List<CohortSectionResponse> sections,
    int weeksInTerm,
    long workingSaturdayCount,
    List<SkeletonClinicalShiftHours> clinicalShiftHours,
    boolean termTimetablePublished,
    List<ClinicalShiftWindow> clinicalShiftWindows
) {}
