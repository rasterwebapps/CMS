package com.cms.dto;

import java.util.List;

/** Real, actually-placed per-day/per-week hours for one faculty in a term — sourced from real
 *  {@code ClassSchedule} rows, distinct from {@link FacultyWorkloadDetail}'s curriculum-derived
 *  totals. {@code byDay} always has all 6 {@code DayOfWeek} entries, even when a day's hours are
 *  0, so a caller never has to guess whether a missing key means zero or unknown. */
public record FacultyScheduleWorkload(
    Long facultyId,
    Long termInstanceId,
    List<DayHours> byDay,
    double weeklyTotalHours
) {
    public record DayHours(String dayOfWeek, double hours) {}
}
