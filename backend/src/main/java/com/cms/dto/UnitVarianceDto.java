package com.cms.dto;

import java.time.LocalDate;

/** Planned (frozen at blueprint-generation time) vs. Projected-or-Actual (live-recomputed against
 *  the current timetable, or the real logged completion date once a unit is marked complete) for
 *  one syllabus unit -- the core "difference" the portion-completion blueprint surfaces. */
public record UnitVarianceDto(
    Long unitId,
    Integer unitNumber,
    String title,
    LocalDate plannedCompletionDate,
    LocalDate projectedOrActualDate,
    boolean completed,
    /** Positive means behind schedule (projected/actual later than planned), negative means
     *  ahead, null when either date is unavailable (e.g. no blueprint generated yet, or the
     *  timetable ran out of sessions before reaching this unit). */
    Integer varianceDays
) {}
