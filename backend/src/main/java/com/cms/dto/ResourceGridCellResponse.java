package com.cms.dto;

import java.time.LocalTime;

import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;

public record ResourceGridCellResponse(
    Long sessionId,
    String subjectName,
    String subjectCode,
    String roomName,
    String facultyName,
    String batchName,
    LocalTime startTime,
    LocalTime endTime,
    String slotName,
    ClassSessionType sessionType,
    ClassScheduleStatus status,
    /** The CourseOffering's curriculum term/semester number (e.g. 1, 3, 5) — this grid pools
     *  sessions from every active cohort's own faculty/room onto one row, so without this a
     *  faculty teaching the same subject code to two different cohorts' terms is indistinguishable
     *  at a glance. Null only if the backing session has no CourseOffering link. */
    Integer termNumber,
    /** True only for a synthetic Clinical Shift cell (bus-depart through bus-return) — it has no
     *  backing {@code ClassSchedule} row, so {@code sessionId} is a negative, non-clickable
     *  placeholder id. See {@code ResourceGridService#toShiftCell}. */
    boolean isOffCampusShift,
    /** The column this cell renders under. For the single-day grid this always equals the one
     *  requested/resolved day; for {@code ResourceGridService#getResourceWeekGrid}'s per-resource
     *  full week, it's the real calendar weekday of that column even when the content underneath
     *  was borrowed from a different day via a {@code DayMappingOverride} — matching how the
     *  single-day Date mode already displays a borrowed schedule under today's own column. */
    DayOfWeek dayOfWeek,
    /** Null for a synthetic Clinical Shift cell (see {@link #isOffCampusShift}) -- the shared
     *  {@code cms-week-grid} component (see WeekGridSession) treats a null periodId as an off-grid
     *  entry and renders it as a spanning block over whichever real Period columns it overlaps,
     *  the same way it already handles that shape for the cohort week views. */
    Long periodId
) {}
