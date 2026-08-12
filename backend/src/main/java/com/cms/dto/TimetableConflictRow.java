package com.cms.dto;

import java.time.LocalTime;
import java.util.List;

import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;

/** One {@code ClassSchedule} row flagged by {@link com.cms.service.TimetableConflictInspectorService}
 *  — carries enough display context (subject/faculty/venue/day/period) for the Conflict Inspector
 *  screen to render a self-contained row without a second lookup per cell. */
public record TimetableConflictRow(
    Long classScheduleId,
    String subjectName,
    String subjectCode,
    ClassSessionType sessionType,
    DayOfWeek dayOfWeek,
    String periodLabel,
    LocalTime startTime,
    LocalTime endTime,
    String facultyName,
    String venueName,
    String cohortLabel,
    ClassScheduleStatus status,
    List<ConstraintViolation> violations
) {
}
