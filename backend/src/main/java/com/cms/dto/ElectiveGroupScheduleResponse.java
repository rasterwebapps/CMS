package com.cms.dto;

import java.time.LocalTime;

import com.cms.model.enums.DayOfWeek;

/** {@code scheduled=false} and every other field null means this term's elective group has no
 *  placed session yet. */
public record ElectiveGroupScheduleResponse(
    boolean scheduled,
    DayOfWeek dayOfWeek,
    String periodName,
    LocalTime startTime,
    LocalTime endTime
) {}
