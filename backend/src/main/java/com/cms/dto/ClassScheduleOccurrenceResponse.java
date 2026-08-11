package com.cms.dto;

import java.time.LocalDate;

import com.cms.model.enums.OccurrenceStatus;

/** One calendar-dated firing of a recurring {@link com.cms.model.ClassSchedule} row, within the
 *  window requested from {@code GET /timetables/occurrences} — {@code occurrenceStatus} is
 *  {@code HELD} for a normal date and {@code CANCELLED} (with {@code cancelReason} set) for a
 *  date the session's period is blocked, so it's shown explicitly rather than silently absent.
 *  {@code SUBSTITUTED} is not distinguished from {@code HELD} here yet — the entry is still built
 *  from the originally-scheduled faculty/room, not {@code SessionOccurrence#effectiveFaculty}. */
public record ClassScheduleOccurrenceResponse(
    LocalDate date,
    ClassScheduleResponse session,
    OccurrenceStatus occurrenceStatus,
    String cancelReason
) {}
