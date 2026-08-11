package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.OccurrenceStatus;

/** One calendar-dated firing of a recurring {@link com.cms.model.ClassSchedule} row, within the
 *  window requested from {@code GET /timetables/occurrences} — {@code occurrenceStatus} is
 *  {@code HELD} for a normal date, {@code CANCELLED} (with {@code cancelReason} set) for a date
 *  the session's period is blocked, and {@code SUBSTITUTED} for a date with a faculty-absence
 *  substitute applied — {@code session.facultyId/facultyName} are overridden to the substitute's
 *  for that date only ({@link com.cms.model.ClassSchedule#getFaculty()} itself is never mutated,
 *  so every other date keeps showing the original faculty). */
public record ClassScheduleOccurrenceResponse(
    LocalDate date,
    ClassScheduleResponse session,
    OccurrenceStatus occurrenceStatus,
    String cancelReason
) {}
