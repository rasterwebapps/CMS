package com.cms.dto;

import java.time.LocalDate;

/** One real calendar-dated firing of a recurring {@link com.cms.model.ClassSchedule} row, within
 *  the window requested from {@code GET /timetables/occurrences}. */
public record ClassScheduleOccurrenceResponse(
    LocalDate date,
    ClassScheduleResponse session
) {}
